# ServiceLink — Session Context & Handoff

A distilled record of the Cowork session working on ServiceLink: project context, decisions made (with rationale), environment gotchas, and what's still open. Cowork chat history does **not** sync across devices, so this file (plus `Backend/ROADMAP.md` and the Git history) is the durable record for resuming on another machine.

`Backend/ROADMAP.md` is the canonical feature/progress tracker. This file complements it with the *why*, the working conventions, and the environment setup that isn't obvious from the code.

---

## 1. Project overview

**ServiceLink** — a Spring Boot REST backend for an IT support **ticketing system**. Users file tickets, agents work them, admins manage users. Three roles: `ADMIN`, `AGENT`, `USER`.

**Tech stack:** Java 17, Spring Boot 3.4.1, Spring Security (JWT), Spring Data JPA / Hibernate 6.6, PostgreSQL, MapStruct (DTO mapping), Lombok, JUnit 5 + Mockito, Testcontainers. Build: Maven (wrapper `./mvnw`).

**Core entities:** `User` (embeds `Credentials` + `Profile`), `Ticket`, `Comment`, `PasswordResetToken`. Enums: `Role`, `TicketStatus`, `TicketPriority`.

---

## 2. Repository structure (important!)

- **The Git repo root is `ServiceLink/`** (this folder). The Maven project lives in the **`Backend/`** subfolder. It's effectively a monorepo layout.
- Consequences:
  - The GitHub Actions workflow lives at **`ServiceLink/.github/workflows/ci.yml`** (repo root), and uses `defaults.run.working-directory: Backend` so build steps run inside the project.
  - `git` commands run from `ServiceLink/`; paths are like `git add Backend/src/...`.
- **`Backend/src/main/resources/application.properties` is gitignored** (contains the dev datasource + a committed dev JWT secret). It exists locally but is **absent in CI** — this caused a real CI failure (see §4).

---

## 3. Environment / local dev setup

- **Running the app locally** needs a local Postgres (dev config points at `localhost:5433`, db `postgres`, user `postgres`, pass `root`).
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
- **Ticket history via Hibernate Envers, not a manual timeline.** Chose `@Audited` over hand-written audit rows in each mutator: the actor is captured centrally by a `RevisionListener` reading the `SecurityContext` at flush time, so no mutator signatures change and every path (incl. bulk) is covered for free. Custom `ServiceLinkRevision` (`@Entity` + `@RevisionEntity`) carries `actorName`/`actorId`. Read side (`getTicketHistory`) diffs consecutive Envers snapshots into per-field `MODIFIED` events + `CREATED`/`DELETED`. `User`-typed relations are `NOT_AUDITED` (FK only); `comments` `@NotAudited`. **Watch-out:** Envers + `@SoftDelete` interact — a soft delete is an UPDATE, so it records as `MOD` (not `DEL`), and the soft-delete flag isn't an audited property → no field-change rows for a delete; verify before relying on the DELETED path.

---

## 6. Working conventions (how this session operated)

- **User usually implements; assistant nudges then reviews.** Common flow: assistant gives a "point me in the right direction" nudge → user implements → user says "check it" → assistant reviews and points out issues (often without fixing until asked).
- **`ROADMAP.md` is kept current** — items marked ✅/🟡/⬜ with a short "what/why" as each lands.
- **Tests accompany every feature.** Service unit tests (Mockito) for logic; `@WebMvcTest` for controllers (with `addFilters=false`, principals seeded into `SecurityContextHolder`, or `any()` matchers for the actor arg). When a service signature changes, update all callers + tests in the same commit.
- **Commit style:** conventional commits (`feat(...)`, `test:`, `fix:`, `docs:`, `ci:`). Assistant generates commit messages on request.
- **Sandbox limitation:** the assistant's shell can't run this project's build (Java 11 / no Maven / no Docker there), so **CI is the test gate** — changes are pushed and verified on GitHub Actions.

---

## 7. Progress status

**Phase 1 — Foundations & security correctness: ✅ COMPLETE**
JWT-derived identity, SecurityConfig ordering fix, role-management endpoint, request validation (+ DTO split), disabled-user login block, password management (change / forgot / reset — forgot uses a logged-token stub, no SMTP yet), consistent soft-delete semantics.

**Phase 2 — Core ticketing: in progress**
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
- ⬜ Remaining Phase 2: **attachments** (last Phase 2 item). History landed first so attachment events can join the timeline later.

**DevOps track**
- ✅ CI test gate + Testcontainers (parity gap closed).
- ⬜ Dockerfile + CD stage; Flyway/Liquibase migrations (to drop `ddl-auto=create-drop`); externalize the datasource + JWT secret out of `application.properties` into env/secrets.

---

## 8. Pending goals / open threads (start here next)

1. ✅ **Ticket history / audit trail — DONE (Hibernate Envers).** Chose Envers `@Audited` over manual writes: a `ServiceLinkRevisionListener` reads the JWT principal from the `SecurityContext` at flush time and stamps `actorName`/`actorId` onto a custom `ServiceLinkRevision`, so every write path (incl. bulk and the mutators that don't take a `UserPrincipal`) is captured with no signature changes. `GET /api/tickets/{id}/history` diffs consecutive Envers snapshots into per-field events (`TicketHistoryEntryDto`), ownership-gated via `assertCanView`. *Open follow-ups:* (a) `@SoftDelete` deletes surface as `MOD` not `DEL` (soft-delete flag isn't audited) → a delete yields no field rows; verify empirically and add a synthetic `DELETED` event if wanted; (b) no Testcontainers integration test yet (create→status→assign→unassign→assert timeline + actor). **Next feature: attachments** — last Phase 2 item.
1. ✅ **Internal-comment follow-up — DONE.** The comments embedded in `TicketResponseDto` now respect the `internal` filter for non-staff: `TicketServiceImpl.hideInternalComments(...)` strips them across every ticket read path (identity preserved when nothing to strip). Covered by owner-hidden / staff-visible tests.
2. ✅ **Unassign endpoint — DONE.** `PATCH /api/tickets/{id}/unassign` → `assignedTo = null` (staff-only via existing URL rules). Service + controller tests.
3. ✅ **`PasswordResetToken` factory — DONE.** `PasswordResetToken.issueFor(user, Duration)` replaces the raw setters in `forgotPassword` (valid-by-construction). *Still open:* `assertCanView` remains duplicated in `TicketServiceImpl` + `CommentServiceImpl` — extract a shared helper if a third copy appears.
4. **Comment endpoint niggles** (non-blocking, still open): default sort on comment reads — consider `createdAt` ascending for conversation order. (Deliberately left out of the last pass as a behavior change.)
5. **Soft-delete caveat** (still open): deleted users still hold the `unique` `username`/`email` constraints (can't re-register those). Revisit if needed (partial unique index or mangling).
6. **JWT lifecycle (Phase 4):** tokens issued before a user is disabled/deleted stay valid until expiry (no refresh/revocation). Add an `isEnabled()` guard in `JwtAuthFilter` if immediate lockout is needed.

*Resolved since last update:* **ticket history / audit trail shipped via Hibernate Envers** (branch `feat/ticket-history-envers`, merged to main) — `@Audited` `Ticket`, actor-capturing `ServiceLinkRevision`/`ServiceLinkRevisionListener`, `GET /api/tickets/{id}/history` diffing snapshots into per-field events. Built on the up-to-date base (the earlier `audited-branch` experiment was on a stale pre-merge commit; superseded). `mvn compile` clean; no integration test yet. *Earlier:* smaller-threads batch — internal-comment leak in `TicketResponseDto` closed, unassign endpoint added, `PasswordResetToken.issueFor` factory. (Branch `worktree-smaller-threads`; full suite green locally — 108 run, 0 fail, 1 skipped = the Docker-gated context test.) *Earlier:* internal vs. public comments (staff-only flag, filtered from list + search); self-assignment & bulk actions (partial-success). Ownership item fully complete. *(Note: three separate `@Query`/param bugs in an earlier session — ticket `searchByKeyword`, comment internal-search — all caught only by the context test. See §3.)*

---

## 9. How to resume (e.g. on Windows)

1. Ensure everything here is committed and pushed (including this file and `ROADMAP.md`).
2. On the new machine: `git clone` the repo (Cowork sessions don't transfer — only the repo does), connect the folder in Cowork.
3. Redo local Docker setup if you want to run the Testcontainers test (Docker Desktop on Windows).
4. Open a new session and point the assistant at `session-context.md` + `Backend/ROADMAP.md` to rebuild context, then continue from §8.
