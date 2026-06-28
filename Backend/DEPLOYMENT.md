# ServiceLink Backend — Deployment Plan

A step-by-step checklist to take this Spring Boot 3.4.1 / Java 17 / Maven service from
"runs on my machine" to "deployable anywhere." Ordered by priority. Each item notes
*why* it matters and *what* to change.

---

## 1. Hard blockers (the app is not deployable until these are done)

### 1.1 Stop dropping the database on every restart
**File:** `src/main/resources/application.properties`
**Current:** `spring.jpa.hibernate.ddl-auto=create-drop`

`create-drop` deletes and recreates the entire schema each time the app starts, so a
production deploy would wipe all data. Change it per environment:

- Dev: `update` (convenient) or keep `create-drop` only in a dev profile.
- Prod: `validate` — Hibernate checks the schema matches your entities but never alters it.
  Schema changes are then handled by a migration tool (see 3.2).

### 1.2 Externalize all configuration to environment variables
**File:** `src/main/resources/application.properties`

Today the datasource URL, DB username/password, and JWT secret are hardcoded literals.
A deployed instance must receive these at runtime. Use Spring's `${ENV:default}` syntax:

```properties
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5433/postgres}
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD:root}

spring.jpa.hibernate.ddl-auto=${JPA_DDL_AUTO:validate}
spring.jpa.show-sql=${JPA_SHOW_SQL:false}

jwt.secret=${JWT_SECRET}
jwt.expiration=${JWT_EXPIRATION:86400000}
```

Defaults after the colon keep local dev working with no env vars set. `JWT_SECRET`
has no default on purpose — the app should refuse to start in prod without a real secret.

### 1.3 Commit a config template; never ship secrets
**File:** `.gitignore` (line 3 currently ignores `application.properties`)

Because `application.properties` is gitignored, a fresh clone or a CI build server has
**no config file**, so the app won't start anywhere but your laptop. Fix it this way:

1. Keep the real secret values out of git (good instinct), but the file with `${ENV}`
   placeholders contains no secrets — so it is safe to commit. Either un-ignore
   `application.properties` once it only holds placeholders, **or** commit an
   `application.properties.example` template and document the required env vars.
2. Generate a fresh production JWT secret (do not reuse the dev one in the repo):
   ```bash
   openssl rand -base64 48
   ```
   Supply it as the `JWT_SECRET` env var on the host.
3. Rotate the committed dev secret if this repo was ever pushed publicly.

### 1.4 Add a Dockerfile (containerize the app)
**New file:** `Dockerfile` (project root)

A multi-stage build keeps the image small and needs no local Maven/JDK on the host:

```dockerfile
# ---- build stage ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -q dependency:go-offline
COPY src/ src/
RUN ./mvnw -q clean package -DskipTests

# ---- run stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**New file:** `.dockerignore` (keeps the build context small and clean):

```
target/
.idea/
.git/
*.iml
HELP.md
```

---

## 2. Should fix before going live

### 2.1 Gate the data seeder to dev only
**File:** `src/main/java/com/wesleyedwards/ServiceLink/Seeder.java`

`Seeder` implements `CommandLineRunner`, so it runs on every startup and will insert
demo users/tickets/comments into your production database. Restrict it to a profile:

```java
@Component
@Profile("dev")          // add this annotation
@RequiredArgsConstructor
public class Seeder implements CommandLineRunner { ... }
```

Then run prod with `SPRING_PROFILES_ACTIVE=prod` so the seeder is skipped.

### 2.2 Make CORS origins environment-driven
**File:** `src/main/java/com/wesleyedwards/ServiceLink/config/SecurityConfig.java`

`setAllowedOrigins(Arrays.asList("http://localhost:4200"))` is hardcoded, so the real
frontend will be blocked. Read it from config instead:

```java
@Value("${cors.allowed-origins:http://localhost:4200}")
private String allowedOrigins;
// ...
configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
```

Then set `CORS_ALLOWED_ORIGINS=https://app.yourdomain.com` (via `cors.allowed-origins`)
in production.

### 2.3 Quiet the SQL logging in production
Already covered by `JPA_SHOW_SQL:false` in 1.2. `show-sql=true` floods logs and hurts
throughput under load.

### 2.4 Provide a local Postgres for parity (optional but recommended)
**New file:** `docker-compose.yml` (project root)

Lets you run the full stack (app + database) with one command and mirrors prod:

```yaml
services:
  db:
    image: postgres:16
    environment:
      POSTGRES_DB: postgres
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: root
    ports: ["5433:5432"]
    volumes: ["pgdata:/var/lib/postgresql/data"]

  app:
    build: .
    depends_on: [db]
    ports: ["8080:8080"]
    environment:
      DB_URL: jdbc:postgresql://db:5432/postgres
      DB_USERNAME: postgres
      DB_PASSWORD: root
      JWT_SECRET: ${JWT_SECRET}
      SPRING_PROFILES_ACTIVE: prod
      JPA_DDL_AUTO: update

volumes:
  pgdata:
```

Run with: `JWT_SECRET=$(openssl rand -base64 48) docker compose up --build`.

---

## 3. Nice to have (hardening and operability)

### 3.1 Add health checks (Spring Boot Actuator)
Cloud platforms probe a health endpoint to know the app is alive. Add to `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Expose just health in `application.properties`:

```properties
management.endpoints.web.exposure.include=health
management.endpoint.health.probes.enabled=true
```

This gives you `/actuator/health` (and liveness/readiness probes for Kubernetes).

### 3.2 Adopt a database migration tool
With `ddl-auto=validate` in prod (1.1), schema changes need a migration tool. Flyway is
the simplest with Spring Boot — add the dependency and put versioned SQL in
`src/main/resources/db/migration` (e.g. `V1__init.sql`). Spring runs them automatically
on startup. This makes schema changes reviewable, repeatable, and reversible.

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

### 3.3 Remove or scope spring-boot-devtools
`spring-boot-devtools` is fine to leave (it auto-disables in packaged jars), but for a
clean prod build you can confirm it's `optional`/`runtime` (it already is in `pom.xml`).

### 3.4 Add a CI workflow
A `.github/workflows/ci.yml` that runs `./mvnw verify` on every push catches build and
test failures before deploy. Extend it later to build and push the Docker image.

### 3.5 Set the server port explicitly
Default is 8080, which the Dockerfile and compose assume. If your platform injects a
`PORT` env var (Heroku, Render), add `server.port=${PORT:8080}`.

---

## Deployment runbook (once the above is in place)

1. Set environment variables on the host/platform:
   `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`,
   `SPRING_PROFILES_ACTIVE=prod`, `CORS_ALLOWED_ORIGINS`, `JPA_DDL_AUTO=validate`.
2. Provision a managed PostgreSQL instance and point `DB_URL` at it.
3. Build the image: `docker build -t servicelink-backend .`
4. Run it (or push to your registry and deploy via the platform).
5. Verify `GET /actuator/health` returns `{"status":"UP"}`.
6. Smoke-test auth: register/login against `/api/users/auth/**` and confirm a JWT is issued.

---

## Quick reference — required environment variables

| Variable | Required | Example | Purpose |
|---|---|---|---|
| `DB_URL` | yes | `jdbc:postgresql://db:5432/postgres` | JDBC connection string |
| `DB_USERNAME` | yes | `postgres` | DB user |
| `DB_PASSWORD` | yes | *(secret)* | DB password |
| `JWT_SECRET` | yes | *(base64, 48+ bytes)* | Signs/verifies JWTs |
| `JWT_EXPIRATION` | no | `86400000` | Token lifetime (ms) |
| `JPA_DDL_AUTO` | recommended | `validate` | Hibernate schema strategy |
| `JPA_SHOW_SQL` | no | `false` | SQL logging toggle |
| `SPRING_PROFILES_ACTIVE` | yes | `prod` | Disables seeder, selects prod config |
| `CORS_ALLOWED_ORIGINS` | yes | `https://app.example.com` | Frontend origin(s), comma-separated |
| `PORT` | platform-dependent | `8080` | HTTP port (if platform injects it) |
