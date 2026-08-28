# Deploying ServiceLink to k3s

Push to `main` → CI tests → images published to GHCR → a self-hosted runner in
the homelab rolls them out. Nothing reaches into your network from outside.

```
GitHub-hosted runners                    Homelab (k3s on Proxmox)
  Build & Test        (backend, 134 tests)
  Frontend Build & Test  (40 tests)
        │  both must pass
        ▼
  Build & Publish Images ──> ghcr.io/<you>/servicelink-{backend,frontend}
        │                                   │
        ▼                                   │ pulled by
  Deploy to homelab  ── self-hosted runner ─┘
        kubectl apply -k deploy/k8s
        kubectl rollout status   ← fails the build if pods never go ready
```

Pull requests stop after the two test jobs. Only pushes to `main` publish or deploy.

---

## One-time setup

### 1. Register the self-hosted runner

On a machine inside the homelab that can reach the cluster:

*Settings → Actions → Runners → New self-hosted runner*, then add the label
`servicelink` when prompted — `ci.yml` targets `[self-hosted, servicelink]`.

The runner needs `kubectl` on its PATH and a kubeconfig with rights to the
`servicelink` namespace. If it runs on a k3s node itself:

```bash
sudo mkdir -p ~actions-runner/.kube
sudo cp /etc/rancher/k3s/k3s.yaml ~actions-runner/.kube/config
sudo chown -R actions-runner: ~actions-runner/.kube
```

Verify as the runner's user: `kubectl get nodes`.

### 2. Create the secret

Never committed — create it directly:

```bash
kubectl create namespace servicelink

kubectl -n servicelink create secret generic servicelink-secrets \
  --from-literal=postgres-user=servicelink \
  --from-literal=postgres-password="$(openssl rand -base64 24)" \
  --from-literal=jwt-secret="$(openssl rand -base64 48)"
```

`jwt-secret` must be long — `JwtUtil` signs with HS256, which wants 256+ bits.
Rotating it invalidates every issued token and logs everyone out, which is
exactly what you want if it leaks.

### 3. Let the cluster pull from GHCR

GHCR packages are **private by default**, and k3s cannot pull them without
credentials. Pick one:

**Simplest —** make both packages public once they exist: *Profile → Packages →
servicelink-backend → Package settings → Change visibility.* Nothing else needed.

**Or, keep them private** with a read-only PAT (`read:packages`):

```bash
kubectl -n servicelink create secret docker-registry ghcr-pull \
  --docker-server=ghcr.io \
  --docker-username=<github-username> \
  --docker-password=<PAT>
```

Then add to both Deployments' `spec.template.spec`:

```yaml
      imagePullSecrets:
        - name: ghcr-pull
```

### 4. Point DNS at the ingress

`deploy/k8s/ingress.yaml` uses `servicelink.home.arpa`. Change it to a hostname
you control and make it resolve to your k3s ingress IP. k3s bundles Traefik and
enables it by default; if you replaced it, change `ingressClassName` to match.

---

## Deploying

Merge to `main`. That's it.

To roll out by hand:

```bash
cd deploy/k8s
sed -i "s|newTag: .*|newTag: <commit-sha>|g" kustomization.yaml
kubectl apply -k .
kubectl -n servicelink rollout status deployment/backend
```

To roll back, apply an older SHA the same way — every image is tagged with the
commit that built it.

---

## How configuration works

The backend image sets `SPRING_PROFILES_ACTIVE=prod`, which loads the committed,
secret-free `application-prod.properties`. That file is **self-contained**:
`src/main/resources/application.properties` is gitignored and therefore absent
from the image, so there is no base layer beneath it.

Required env vars, all supplied by the Deployment:

| Variable | Source | Notes |
|---|---|---|
| `SPRING_DATASOURCE_URL` | manifest | `jdbc:postgresql://postgres:5432/servicelink` |
| `SPRING_DATASOURCE_USERNAME` | secret | |
| `SPRING_DATASOURCE_PASSWORD` | secret | |
| `JWT_SECRET` | secret | no default — the app refuses to start without it |
| `JWT_EXPIRATION` | optional | defaults to 24h |
| `MAX_UPLOAD_SIZE` | optional | defaults to 10MB |
| `CORS_ALLOWED_ORIGINS` | optional | empty by design, see below |

Anything without a default fails the boot loudly rather than falling back to
something that happens to work locally.

### Why there is no CORS configuration

The Ingress serves the UI and the API from one host — `/` to the frontend,
`/api` to the backend — so the browser never makes a cross-origin request. The
production Angular build sets `apiUrl: ''`, making every call same-origin
(`/api/tickets`).

Two problems vanish: no CORS to misconfigure, and no API hostname compiled into
the frontend image, so the same image runs in any cluster without a rebuild.
Only set `CORS_ALLOWED_ORIGINS` if you serve the UI from a different host.

### The database schema

Flyway owns it. `deploy`-time migrations run at application startup from
`db/migration`, and `ddl-auto` is `validate` — so an entity change without a
matching migration **fails the pod at boot** instead of silently altering a live
database.

`V1__baseline_schema.sql` was generated by running Hibernate against a clean
Postgres and dumping the result, not written by hand — the Envers audit tables
and identity sequences are easy to get subtly wrong.

To change the schema: edit the entity, add `V2__<description>.sql`, deploy. Never
edit an applied migration; Flyway checksums them and will refuse to start.

---

## Troubleshooting

**Pods stuck `ImagePullBackOff`** — the packages are still private. See step 3.

**Backend crashlooping, logs mention Flyway checksum** — an applied migration was
edited. Add a new one instead; `kubectl -n servicelink exec statefulset/postgres --
psql -U servicelink -d servicelink -c 'select * from flyway_schema_history'` shows
what was applied.

**Backend never ready, initContainer looping** — Postgres isn't up. Check the
StatefulSet and that its PVC bound (`kubectl -n servicelink get pvc`); k3s needs a
`local-path` StorageClass, or change it in `postgres.yaml`.

**Deploy job can't reach the cluster** — the runner's kubeconfig. Run
`kubectl get nodes` as the runner's user.

**404 on refresh of a deep link like `/tickets/42`** — nginx SPA fallback. It is
configured in `Frontend/ServiceLink/nginx.conf`; check the built image has it.

---

## What this does not do

- **No TLS here.** The Ingress speaks plain HTTP and expects your existing proxy
  to terminate. Add a `tls:` block and a cert-manager issuer if you want it
  handled in-cluster.
- **No backups.** The PVC is the only copy of the data. `pg_dump` on a schedule
  before this holds anything you care about.
- **Single replica each.** Both Deployments are `replicas: 1`; the backend is
  stateless and could scale, Postgres as configured cannot.
- **No staging.** One environment. A second would mean a second namespace, its
  own secret, and a branch or tag to trigger it.
