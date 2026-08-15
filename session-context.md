# ServiceLink — Session Context & Handoff

A distilled record of the Cowork session working on ServiceLink: project context, decisions made (with rationale), environment gotchas, and what's still open. Cowork chat history does **not** sync across devices, so this file (plus `Backend/ROADMAP.md` and the Git history) is the durable record for resuming on another machine.

`Backend/ROADMAP.md` is the canonical feature/progress tracker. This file complements it with the *why*, the working conventions, and the environment setup that isn't obvious from the code.

---

## 1. Project overview

**ServiceLink** — an IT support **ticketing system**: a Spring Boot REST backend plus an Angular single-page frontend. Users file tickets, agents work them, admins manage users. Three roles: `ADMIN`, `AGENT`, `USER`.

**Backend stack:** Java 17, Spring Boot 3.4.1, Spring Security (JWT), Spring Data JPA / Hibernate 6.6, PostgreSQL, MapStruct (DTO mapping), Lombok, JUnit 5 + Mockito, Testcontainers. Build: Maven (wrapper `./mvnw`).

**Frontend stack:** Angular 21 (standalone components, `@if`/`@for` control flow), Tailwind 4, vitest. Build: npm. Lives in `Frontend/ServiceLink/` and is **a tracked deliverable**, not scaffolding — see §7.

**Core entities:** `User` (embeds `Credentials` + `Profile`), `Ticket`, `Comment`, `Attachment`, `PasswordResetToken`. Enums: `Role`, `TicketStatus`, `TicketPriority`.

---

## 2. Repository structure (important!)

- **The Git repo root is `ServiceLink/`** (this folder). The Maven project lives in the **`Backend/`** subfolder and the Angular app in **`Frontend/ServiceLink/`**. It's effectively a monorepo layout.
- ⚠️ **The frontend directory's on-disk name and its tracked name differ in case.** Git tracks it as **`Frontend/ServiceLink/`** (all 75 files, consistently), but the local working copy on this Mac is **`Frontend/servicelink/`** — macOS's case-insensitive filesystem means Git never sees a difference, and `git status` will print either spelling depending on how you `cd` there. The repo itself is internally consistent, so **a fresh clone on Linux materialises `Frontend/ServiceLink/`** and everything works. The trap is scripts or CI jobs that hardcode the lowercase path from a local shell — those break on a case-sensitive checkout. Use the tracked spelling (`Frontend/ServiceLink`) in anything committed.
- Consequences:
  - The GitHub Actions workflow lives at **`ServiceLink/.github/workflows/ci.yml`** (repo root), and uses `defaults.run.working-directory: Backend` so build steps run inside the project.
  - `git` commands run from `ServiceLink/`; paths are like `git add Backend/src/...`.
- **`Backend/src/main/resources/application.properties` is gitignored** (contains the dev datasource + a committed dev JWT secret). It exists locally but is **absent in CI** — this caused a real CI failure (see §4).

---

## 3. Environment / local dev setup

- **Running the backend locally** needs a local Postgres (dev config points at `localhost:5433`, db `postgres`, user `postgres`, pass `root`).
- **Frontend loop — no Docker, no Postgres, no Java.** `cd Frontend/ServiceLink && npm install && npm start` (serves on `localhost:4200`; `npm run build`, `npm test` for the rest). **`node_modules` is not committed and starts empty**, so `npm install` is the first step on any machine. The Docker requirement below is backend-Testcontainers only and does not apply here. Note `npm test` runs vitest once and exits — there is no `--run` flag to pass (`ng test --run` errors with *Unknown argument*).
- **The frontend talks to `http://localhost:8080`** (`src/environments/environment.ts`). `SecurityConfig`'s CORS block allows exactly `http://localhost:4200`, so serving the frontend on any other port fails preflight rather than 404ing — a confusing symptom if you change the dev port.
- **Running the full test suite locally needs Docker** because the `@SpringBootTest` context test (`ServiceLinkApplicationTests`) uses Testcontainers. The other tests (Mockito unit tests, `@WebMvcTest` slices) do **not** need Docker.
- **Docker on this Mac = Colima** (not Docker Desktop). Getting Testcontainers to talk to Colima required, in `~/.zshrc`:
  ```bash
  export DOCKER_HOST="unix://$HOME/.colima/default/docker.sock"
  export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE="/var/run/docker.sock"
  export DOCKER_API_VERSION=1.43   # Colima's daemon (API 1.54, min 1.40) rejects the client's default 1.32
  ```
  Also `~/.testcontainers.properties` with `docker.host=unix:///Users/wesleyedwards/.colima/default/docker.sock` as a shell-independent fallback.
- **On Windows:** install Docker Desktop for Windows for the Testcontainers test; otherwise the context test auto-skips (see §5, `@EnabledIf`).
- The context test is guarded with `@EnabledIf("dockerAvailable")`, so a machine without Docker **skips** it (build stays green) while CI runs it.
- **`@Query` HQL is only validated by the full `@SpringBootTest` context test.** The Mockito service tests and `@WebMvcTest` slices mock/exclude the repository, so a malformed `@Query` compiles and passes them silently — it only blows up when the JPA context boots (repository bean creation → `QueryCreationException` → whole context fails). *Real example:* a stray comment-search query (`c.ticket.id`, `c.content`) got pasted into `TicketRepository.searchByKeyword`; every unit test passed, but the context test failed. **Always run the context test (Docker up) before trusting a repository/`@Query` change** — don't rely on the unit tests to catch it.

---

## 4. CI/CD

- **GitHub Actions** (`.github/workflows/ci.yml`): on every push/PR, `ubuntu-latest`, JDK 17 (Temurin, Maven cache), runs `./mvnw -B --no-transfer-progress verify` from `Backend/`.
- **Testcontainers** provides Postgres for the context test via `@ServiceConnection` — no service-container block or datasource env in the workflow; the test owns its DB. Local/CI parity.
- **Lesson learned:** because `application.properties` is gitignored, CI had no `ddl-auto` → Postgres defaulted to `none` → no schema → the seeder's query hit a missing `user_table`. Fix: a **committed** `Backend/src/test/resources/application.properties` (self-contained test config: `ddl-auto=create-drop`, JWT placeholders with defaults, Jackson settings, `spring.profiles.active=test`). A test-scoped `application.properties` **shadows** the main one during tests, so it must be complete.
- **Optional but recommended `JWT_SECRET`** GitHub Actions secret (tests pass without it; it's the pattern for real deploys).

---

## 5. Key decisions made (with rationale)

- **Identity from the JWT, not request params.** Introduced `UserPrincipal implements UserDetails` (in `config/`) carrying `userId` + `role`; returned from `UserDetailsServiceImpl`. Controllers read the actor via `@AuthenticationPrincipal`. Replaced spoofable `?requesterId=`/`?authorId=` params.
- **`UserPrincipal` is a wrapper (Option A), not the entity.** Keeps Spring Security concerns out of the JPA entity. Has helpers `getUserId()`, `getRole()`, `isAdmin()`, `isStaff()` (admin OR agent).
- **Login goes through `AuthenticationManager`** (`DaoAuthenticationProvider`), not a manual password check — gets `isEnabled()` (disabled-user block) and bad-credentials handling for free; identity read off the returned `Authentication` principal (one DB hit).
- **Exceptions → status:** `BadRequestException`→400, `NotFoundException`→404, `NotAuthorizedException`→401, `BadCredentialsException`→401, `DisabledException`→403, `ForbiddenException`→403 (new, for "authenticated but not permitted"). Handled in `ServiceLinkControllerAdvice`.
- **`SecurityConfig` rule ordering matters** (first match wins). Method-specific rules go **before** broad ones, or they become dead code (this caused a USER 403 bug). Current model: USER can GET/POST tickets & comments and PUT (edit) comments; PUT/PATCH/DELETE otherwise ADMIN/AGENT.
- **Create/Update DTO split for validation.** Strict constraints on create DTOs; separate `ProfileUpdateDto`/`TicketUpdateDto` with null-tolerant constraints so PATCH partial updates aren't rejected. `@Valid` on every `@RequestBody`; validation errors → 400 via `MethodArgumentNotValidException` handler.
- **Consistent soft delete** via Hibernate `@SoftDelete` on `User`, `Ticket`, `Comment` (NOT `Profile` — it's an `@Embeddable`). `delete()` becomes an UPDATE; queries auto-filter. `deleteuser` now truly soft-deletes; `isDisabled` was repurposed as a distinct **suspend/ban** feature (login enforcement already wired).
- **Ticket status is a state machine.** `TicketStatus.canTransitionTo(target)` (switch-based). New tickets default to `NEW` (entity field initializer + `status` removed from create/update DTOs). Status changes only via `PATCH /api/tickets/{id}/status` (validated); illegal transitions → 400.
- **Ownership authorization is service-layer** (not `@PreAuthorize`/SpEL) for testability. Policy: modify tickets = admin + **any** agent (already covered by URL rules); reads restricted so a USER sees only their own tickets. Profiles: edit = self or ADMIN (agents excluded). Comments: authors can **edit** their own within a **15-minute window** (staff bypass); delete stays staff-only.
- **Pagination:** `@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)` on the main class → stable `PagedModel` JSON (content at `$.content`, metadata under `$.page`). Clients must read `$.page.totalElements`, not flat `$.totalElements`.
- **Internal vs. public comments.** `internal` boolean on `Comment` (default public). Only staff can set it — `addCommentToTicket` forces `internal = actor.isStaff() && request.internal()`. Reads filter it out for non-staff via `...InternalFalse` repo variants in both list *and* search; both also gate ticket ownership (`assertCanView`).
- **Bulk actions = partial success, deliberately NOT `@Transactional`.** `PUT /bulk/status` + `PATCH /bulk/assign` return `BulkResultDto(succeeded, failed)` with per-item try/catch. A wrapping transaction would roll back the successes when a later item throws, so each `saveAndFlush` auto-commits independently. Bulk assign validates the assignee once (batch-level 404); bulk status reuses `canTransitionTo` and reports bad-id + illegal-transition per item.
- **The frontend mirrors `canTransitionTo` rather than asking the server.** `ALLOWED_TRANSITIONS` in `models/ticket.model.ts` duplicates the Java state machine, which is exactly the kind of drift the resync existed to remove — accepted deliberately because there is no endpoint exposing legal moves yet. Mitigated two ways: a spec asserts the map equals the backend table (drift fails a test, rather than silently dropping a move from the UI), and the 400 handler is kept even though the UI cannot compose an illegal transition. **If you add a transition to the enum, a frontend test goes red — that is intended.** The real fix is exposing transitions from the server (Phase 3/4 candidate).
- **The UI encodes priority in colour and status in luminance.** One rule: chroma is reserved for urgency. The four saturated palette values map 1:1 onto `TicketPriority`; ticket *status* uses brightness only, so the two axes stay independently readable and nothing decorative can borrow an urgency colour. Full rationale in the header comment of `Frontend/ServiceLink/src/styles.css`.
- **No SLA field exists, so the UI measures dwell, not deadlines.** `Ticket` carries only `createdAt`/`updatedAt`. Elapsed time is rendered as "quiet for" (time since last change) and never as "due in" or "breaching" — a countdown against a non-existent target would be invented data presented as measurement. `formatElapsed` in `shared/util/time.ts` carries the constraint in its doc comment. Adding real SLAs is a backend change first (Phase 3).
- **Ticket history via Hibernate Envers, not a manual timeline.** Chose `@Audited` over hand-written audit rows in each mutator: the actor is captured centrally by a `RevisionListener` reading the `SecurityContext` at flush time, so no mutator signatures change and every path (incl. bulk) is covered for free. Custom `ServiceLinkRevision` (`@Entity` + `@RevisionEntity`) carries `actorName`/`actorId`. Read side (`getTicketHistory`) diffs consecutive Envers snapshots into per-field `MODIFIED` events + `CREATED`/`DELETED`. `User`-typed relations are `NOT_AUDITED` (FK only); `comments` `@NotAudited`. **Watch-out:** Envers + `@SoftDelete` interact — a soft delete is an UPDATE, so it records as `MOD` (not `DEL`), and the soft-delete flag isn't an audited property → no field-change rows for a delete; verify before relying on the DELETED path.

---

## 6. Working conventions (how this session operated)

- **User usually implements; assistant nudges then reviews.** Common flow: assistant gives a "point me in the right direction" nudge → user implements → user says "check it" → assistant reviews and points out issues (often without fixing until asked).
- **`ROADMAP.md` is kept current** — items marked ✅/🟡/⬜ with a short "what/why" as each lands.
- **Tests accompany every feature.** Service unit tests (Mockito) for logic; `@WebMvcTest` for controllers (with `addFilters=false`, principals seeded into `SecurityContextHolder`, or `any()` matchers for the actor arg). When a service signature changes, update all callers + tests in the same commit.
- **Commit style:** conventional commits (`feat(...)`, `test:`, `fix:`, `docs:`, `ci:`). Assistant generates commit messages on request.
- **Sandbox limitation (backend):** the assistant's shell can't run the Maven build (Java 11 / no Maven / no Docker there), so **CI is the test gate** — changes are pushed and verified on GitHub Actions.
- **The frontend is different: the assistant *can* run it.** `npm install` / `npm run build` / `npm test` all work locally, so frontend changes are verified before commit rather than on CI. **There is no frontend CI job yet** — `.github/workflows/ci.yml` only runs `./mvnw verify` from `Backend/`, so nothing gates the Angular build on a PR. Worth adding.
- **What the assistant still cannot do: see the rendered page.** No browser tooling in the session, so visual results are reasoned about, not observed. This is not theoretical — a modal shipped mis-positioned because `.animate-settle`'s keyframe ends at `transform: none` with `fill-mode: both`, which permanently overrode the `transform: translate(-50%, -50%)` centring on the same element. It built clean and passed every test. **Have a human look at any new layout before calling it done**, and prefer flex/grid centring over transform on anything that also animates.

---

## 7. Progress status

**Phase 1 — Foundations & security correctness: ✅ COMPLETE**
JWT-derived identity, SecurityConfig ordering fix, role-management endpoint, request validation (+ DTO split), disabled-user login block, password management (change / forgot / reset — forgot uses a logged-token stub, no SMTP yet), consistent soft-delete semantics.

**Phase 2 — Core ticketing: ✅ COMPLETE**
- ✅ Richer status lifecycle (statuses + transition state machine + `PATCH /{id}/status`).
- ✅ Ownership-based authorization (COMPLETE):
  - ✅ Tickets (read ownership; modify stays admin+agent).
  - ✅ Profiles (edit = self or admin).
  - ✅ Comments (author edit within 15-min window; delete staff-only).
  - ✅ Status/priority/search endpoints scoped to requester for non-staff (nullable `requesterId` in the repo queries; `null` = staff/unscoped — keeps pagination totals correct). Verified in CI.
- ✅ Comment search & pagination: `GET /api/comments/ticket/{id}` paginated + `GET /api/comments/ticket/{id}/search` (keyword over `content`, per-ticket, repo-level JPQL). Both guarded staff-or-requester (403 otherwise) — also closed a pre-existing gap where any USER could read any ticket's comments. `assertCanView` duplicated into `CommentServiceImpl` (extract on third copy).
- ✅ Internal vs. public comments: `internal` flag on `Comment`, staff-only to set, filtered out of list + search for non-staff.
- ✅ Self-assignment & bulk actions: bulk status (`PUT /bulk/status`) + bulk assign (`PATCH /bulk/assign`), partial-success `BulkResultDto`. Assign-to-me dropped (no new capability); ✅ **unassign** now added (`PATCH /api/tickets/{id}/unassign` → `assignedTo = null`, staff-only).
- ✅ Internal-comment leak in ticket responses closed: `TicketResponseDto` embeds the full comment list, so `TicketServiceImpl` now strips internal comments for non-staff across every ticket read path (the comment endpoints already filtered; the ticket mapper didn't).
- ✅ Ticket history / audit trail: Hibernate Envers (`@Audited` on `Ticket`) + custom `ServiceLinkRevision`/`ServiceLinkRevisionListener` capturing the JWT actor at flush time; `GET /api/tickets/{id}/history` (ownership-gated) diffs Envers snapshots into per-field events. *(Soft-delete shows as MOD not DEL — flagged in-code; no integration test yet.)*
- ✅ **Attachments (last Phase 2 item, `d7c9e07`):** upload to ticket or comment (multipart `file` part), list, download, delete. `AttachmentService` enforces ticket ownership and the internal-comment rule, so a USER can't reach a file on a staff-only note. Stored as bytes in the DB — object storage is a Phase 4 concern if volume grows.

**Frontend track — 🟡 caught up** (see `FRONTEND-RESYNC-PLAN.md`, closed through step 5)
- ✅ API resync complete (PRs #8, #9, #14) — verbs, six-state lifecycle, `$.page` shape, JWT identity.
- ✅ UI for every Phase 1–2 capability (PR #14): status transitions via the lifecycle rail, audit trail, attachments, internal comments, unassign, role management, filterable + paginated search.
- ✅ Design system (PR #14) — see §5 for the colour rule and the no-SLA constraint.
- ✅ Suite went 9 passing / 3 failing → **40 passing across 13 files**; build warning-free.
- ⬅️ **End-to-end verification never run** — nobody has driven a ticket through the state machine in a browser with the API up. This is the outstanding gate.
- ⬜ No UI yet: bulk status/assign, comment search, password change/forgot/reset, suspend/ban, registration.
- ⬜ No frontend CI job — nothing gates the Angular build on a PR.

**DevOps track**
- ✅ CI test gate + Testcontainers (parity gap closed) — **backend only**.
- ⬜ Dockerfile + CD stage; Flyway/Liquibase migrations (to drop `ddl-auto=create-drop`); externalize the datasource + JWT secret out of `application.properties` into env/secrets.
- ⬜ Add an Angular job to `ci.yml` (`npm ci && npm run build && npm test` from `Frontend/ServiceLink`) — note the tracked path casing in §2.

---

## 8. Pending goals / open threads (start here next)

1. ✅ **Ticket history / audit trail — DONE (Hibernate Envers).** Chose Envers `@Audited` over manual writes: a `ServiceLinkRevisionListener` reads the JWT principal from the `SecurityContext` at flush time and stamps `actorName`/`actorId` onto a custom `ServiceLinkRevision`, so every write path (incl. bulk and the mutators that don't take a `UserPrincipal`) is captured with no signature changes. `GET /api/tickets/{id}/history` diffs consecutive Envers snapshots into per-field events (`TicketHistoryEntryDto`), ownership-gated via `assertCanView`. ✅ *Follow-ups closed:* (a) soft-delete is now viewable — `getTicketHistory` derives existence + ownership from the Envers history (not the `@SoftDelete`-filtered `findById`, which 404'd) and ends the timeline with a `DELETED` event, robust to Envers recording the delete as `DEL` or as a `MOD` on the non-audited flag (synthesized from the terminal revision); (b) Testcontainers integration test added (`TicketHistoryTest` + shared `AbstractPostgresIT` base) driving create→status→assign→unassign→delete and asserting timeline/actor/ordering/DELETED — green on CI. *Gotchas found writing it:* `@EnabledIf` is not `@Inherited` (Docker gate must sit on each concrete IT, not just the base, or it errors instead of skipping); name ITs `*Test` not `*IT` (no Failsafe config, Surefire only picks up `*Test`/`*Tests`); the single-entity mutators used to lean on open-session-in-view (a direct service call tripped `LazyInitializationException` on `Ticket.comments` via the mapper). ✅ *Resolved:* `updateTicketStatus`/`assignTicketToUser`/`unassignTicket` are now `@Transactional` (like `updateTicket`), so they no longer depend on OSIV, and `TicketHistoryTest` calls them directly (the `TransactionTemplate` workaround was removed). **Next feature: attachments** — last Phase 2 item.
1. ✅ **Internal-comment follow-up — DONE.** The comments embedded in `TicketResponseDto` now respect the `internal` filter for non-staff: `TicketServiceImpl.hideInternalComments(...)` strips them across every ticket read path (identity preserved when nothing to strip). Covered by owner-hidden / staff-visible tests.
2. ✅ **Unassign endpoint — DONE.** `PATCH /api/tickets/{id}/unassign` → `assignedTo = null` (staff-only via existing URL rules). Service + controller tests.
3. ✅ **`PasswordResetToken` factory — DONE.** `PasswordResetToken.issueFor(user, Duration)` replaces the raw setters in `forgotPassword` (valid-by-construction). ✅ *Resolved:* the duplicated `assertCanView` was extracted into a `TicketAccessPolicy` interface + `TicketAccessPolicyImpl` (`@Service`), injected into both `TicketServiceImpl` and `CommentServiceImpl` (four call sites delegate; private copies removed). Tests use a real `@Spy new TicketAccessPolicyImpl()` so the 403 assertions still fire.
4. ✅ **Comment endpoint niggles — DONE.** Comment reads now default to `createdAt` **ascending** (conversation order, oldest first) on both `GET /api/comments/ticket/{ticketId}` and `.../search` — hardcoded, no `sortDir` param (kept the change to a one-word diff; add the param later if a caller actually needs it). **Tickets deliberately stay descending** — newest-first is right for a list view. *This is an API contract change the Angular frontend depends on: comment lists now render top-to-bottom in conversation order without client-side reversing.* Also: added the missing leading slash on the search mapping, removed stale TODO markers, and dropped a dead commented-out line in `TicketRepository`. `CommentControllerTest` captures the `Pageable` and asserts `Sort.Direction.ASC` (confirmed to fail if the sort is flipped back), since nothing previously pinned the direction — the tests matched with `any(Pageable.class)`.
5. ✅ **DONE (through step 5): frontend API resync — see [`FRONTEND-RESYNC-PLAN.md`](FRONTEND-RESYNC-PLAN.md) (repo root).** `Frontend/ServiceLink/` is a real Angular 21 app (Tailwind 4, vitest) that this file never tracked — and it was written against the *pre-Phase-1* API, so it had drifted badly. Sequenced **before attachments** because attachments need both halves anyway, and because a working UI is a better contract test than another mocked suite (cf. the `@Query` bugs in §3 that only the context test caught). `npm install` first — `node_modules` starts empty; no Docker needed for frontend work.
   - ✅ **Step 1 — models (PR #8, `7a3455e`):** `Status` filled out to all 6 backend values (the extras had been added to `Priority` by mistake, and `ON_HOLD` spelled with a space), `Role` corrected to `ADMIN`/`AGENT`/`USER`, `PageResponse` reshaped to `VIA_DTO`'s nested `$.page`, `UserResponse` filled in, `status` dropped from `TicketRequest` + the ticket form.
   - ✅ **Step 2 — services (PR #9, `91d1fb9`):** `assignTicket` and `updateComment` `PATCH` → `PUT` (both were 405s; `SecurityConfig` also only admits a USER on `PUT /api/comments/**`); `updateTicketStatus` added for `PATCH /{id}/status` with a `{ ticketStatus }` body; the dead `?requesterId=` / `?authorId=` params and their arguments removed (identity comes from the JWT principal); `getCommentsForTicket` retyped to `Observable<PageResponse<CommentResponse>>` with `page`/`size`.
   - ✅ **Steps 3–5 — components, hygiene, tests (PR #14, `e381dd2`).** Status is now changeable via the **lifecycle rail**, which renders `canTransitionTo` as a diagram and offers only legal moves. The `/api/users/null` request is guarded (and pinned by a spec), badges cover all six states from one exported map, `openTickets` counts all four non-terminal states, the token logging is gone, and the specs have real TestBeds asserting HTTP method/URL/body-key. `ticket-detail` now reads the sorted comment endpoint, so the missing-`@OrderBy` problem no longer affects that view (the embedded list is still unordered for other consumers).
   - **Five defects the plan's inventory missed, found while implementing.** Recorded in full in the plan doc; the two worth carrying here: (a) **`ticket-list` never rendered `<app-ticket-form>`** — the New Ticket button set a flag nothing read, so the modal could not open. This was visible the whole time as an `NG8113` warning that had been triaged as a harmless unused import. **`NG8113` on a *component* usually means a feature is unreachable, not that an import is dead.** (b) **`/admin/users` had no route** despite the sidebar linking to it, so the wildcard bounced admins to the login screen.
   - ⬅️ **Outstanding: end-to-end verification (plan §3).** Build clean, 40 specs green, and the new-ticket modal confirmed in a browser — but nobody has walked a ticket `NEW → IN_PROGRESS → RESOLVED → CLOSED → REOPENED` against a live backend, or forced an illegal transition to confirm the 400 surfaces. **Do this before treating the resync as closed.**
6. **Soft-delete caveat** (still open): deleted users still hold the `unique` `username`/`email` constraints (can't re-register those). Revisit if needed (partial unique index or mangling).
7. **JWT lifecycle (Phase 4):** tokens issued before a user is disabled/deleted stay valid until expiry (no refresh/revocation). Add an `isEnabled()` guard in `JwtAuthFilter` if immediate lockout is needed.

8. **Two backend DTO gaps, both now with concrete frontend consumers** (new, 2026-08-15): `TicketResponseDto` returns `assignedTo`/`requester` as bare UUIDs, so list views resolve names only for ADMINs (who can call the admin-only `GET /api/users`) — AGENTs and USERs see "Assigned"/"Unassigned". And `UserResponseDto` doesn't project `role` at all (it lives on `Credentials`), so the people page shows a person's role as "not shown" until an admin sets one. Small additions, worth doing together.

*Resolved since last update (2026-08-15):* **the frontend resync closed through step 5, plus a full visual redesign** — PR #14 (`e381dd2`), branch `feat/frontend-instrumentation-design`. Every Phase 1–2 backend capability now has a UI; several (audit trail, attachments, unassign, role management, internal comments) had no client at all before. Suite went 9 passing / 3 failing → 40 passing across 13 files, build warning-free. *Earlier:* **attachments shipped** (`d7c9e07`), completing Phase 2. *Earlier:* **ticket history / audit trail shipped via Hibernate Envers** (branch `feat/ticket-history-envers`, merged to main) — `@Audited` `Ticket`, actor-capturing `ServiceLinkRevision`/`ServiceLinkRevisionListener`, `GET /api/tickets/{id}/history` diffing snapshots into per-field events. Built on the up-to-date base (the earlier `audited-branch` experiment was on a stale pre-merge commit; superseded). `mvn compile` clean; no integration test yet. *Earlier:* smaller-threads batch — internal-comment leak in `TicketResponseDto` closed, unassign endpoint added, `PasswordResetToken.issueFor` factory. (Branch `worktree-smaller-threads`; full suite green locally — 108 run, 0 fail, 1 skipped = the Docker-gated context test.) *Earlier:* internal vs. public comments (staff-only flag, filtered from list + search); self-assignment & bulk actions (partial-success). Ownership item fully complete. *(Note: three separate `@Query`/param bugs in an earlier session — ticket `searchByKeyword`, comment internal-search — all caught only by the context test. See §3.)*

---

## 9. How to resume (e.g. on Windows)

1. Ensure everything here is committed and pushed (including this file, `ROADMAP.md`, and `FRONTEND-RESYNC-PLAN.md`).
2. On the new machine: `git clone` the repo (Cowork sessions don't transfer — only the repo does), connect the folder in Cowork.
3. Redo local Docker setup if you want to run the Testcontainers test (Docker Desktop on Windows). **Not needed for frontend work.**
4. **For frontend work:** `cd Frontend/ServiceLink && npm install` — `node_modules` is not committed and starts empty. Then `npm start` (needs the backend on `:8080`, and the frontend must serve on `:4200` for CORS).
5. Open a new session and point the assistant at `session-context.md` + `Backend/ROADMAP.md` (+ `FRONTEND-RESYNC-PLAN.md` for frontend work) to rebuild context, then continue from §8.

**Immediate next step, whichever machine you're on:** run the end-to-end verification in `FRONTEND-RESYNC-PLAN.md` §3. It is the one gate standing between "builds and tests green" and "known good", and it needs a human with a browser.
