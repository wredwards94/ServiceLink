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
- ⬜ Remaining Phase 2 (agreed order): internal vs. public comments → self-assignment & bulk actions → ticket history/audit trail → attachments. (History before attachments so attachment events land in the timeline from day one.)

**DevOps track**
- ✅ CI test gate + Testcontainers (parity gap closed).
- ⬜ Dockerfile + CD stage; Flyway/Liquibase migrations (to drop `ddl-auto=create-drop`); externalize the datasource + JWT secret out of `application.properties` into env/secrets.

---

## 8. Pending goals / open threads (start here next)

1. **Next feature: internal vs. public comments** (first of the agreed Phase 2 sequence: internal comments → self-assignment & bulk actions → ticket history → attachments). Visibility flag on `Comment`, filter reads for non-staff — including the comments embedded in `TicketResponseDto`, not just the comment endpoints.
2. **Comment endpoint niggles** (from the search/pagination review, non-blocking): default sort is `createdAt` descending — consider ascending for conversation order; add leading slashes to the new `ticket/...` mappings; delete the commented-out old `getCommentsForTicket`; remove the orphaned `List<Comment> findAllByTicketId(Long)` overload and unused `List` imports.
3. **Deferred refactor:** replace `PasswordResetToken`'s raw setters in `forgotPassword` with a static factory (`PasswordResetToken.issueFor(user, Duration)`) — valid-by-construction (`token`/`expiresAt` are non-null columns). Also: `assertCanView` now duplicated in `TicketServiceImpl` + `CommentServiceImpl` — extract a shared helper if a third copy appears.
4. **Soft-delete caveat:** deleted users still hold the `unique` `username`/`email` constraints (can't re-register those). Revisit if needed (partial unique index or mangling).
5. **JWT lifecycle (Phase 4):** tokens issued before a user is disabled/deleted stay valid until expiry (no refresh/revocation). Add an `isEnabled()` guard in `JwtAuthFilter` if immediate lockout is needed.

*Resolved since last update:* ticket search/status ownership gap (requester-scoped repo queries, CI-verified); `VIA_DTO` pagination assertion confirmed green; comment search & pagination shipped with staff-or-requester guards (closing the open comment-read hole).

---

## 9. How to resume (e.g. on Windows)

1. Ensure everything here is committed and pushed (including this file and `ROADMAP.md`).
2. On the new machine: `git clone` the repo (Cowork sessions don't transfer — only the repo does), connect the folder in Cowork.
3. Redo local Docker setup if you want to run the Testcontainers test (Docker Desktop on Windows).
4. Open a new session and point the assistant at `session-context.md` + `Backend/ROADMAP.md` to rebuild context, then continue from §8.
