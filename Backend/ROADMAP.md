# ServiceLink — Feature Roadmap

A prioritized plan for evolving the ServiceLink IT ticketing backend (Spring Boot 3.4, Java 17, PostgreSQL, JWT auth). Items are grouped into four phases by impact, risk, and dependency order. Earlier phases unblock later ones — notably, deriving identity from the JWT (Phase 1) is a prerequisite for trustworthy ownership rules, notifications, and analytics.

Each item notes the relevant code so work can start quickly.

---

## Phase 1 — Foundations & security correctness ✅ COMPLETE

These close gaps that affect data integrity and trust. They are relatively small but high-impact, and several later features depend on them. All items done: JWT-derived identity, SecurityConfig ordering fix, role management, request validation, disabled-user login block, password management (change / forgot / reset), and consistent soft-delete semantics.

**Derive the acting user from the JWT, not request params.** ✅ *Done.*
`createTicket` and `addComment` previously took `requesterId` / `authorId` as query params, so a caller could act as any user. Introduced a `UserPrincipal` (implements `UserDetails`, carries the `UUID`), returned it from `UserDetailsServiceImpl`, and the controllers now read the actor via `@AuthenticationPrincipal`. Controller tests updated to seed the principal into the security context. This is a prerequisite for ownership rules and notifications. *(Assignment endpoints still take an explicit `userId` — that refers to the assignee, not the actor, which is correct.)*

**Fix `SecurityConfig` rule ordering.** ✅ *Done.*
Rules are evaluated top-to-bottom (first match wins), and the broad `/api/tickets/**` and `/api/comments/**` rules (matching all HTTP methods, ADMIN/AGENT only) sat *before* the method-specific rules that included USER — so the USER-inclusive POST/GET rules were dead code and a USER got 403 creating a ticket. Reordered so the method-specific GET/POST rules (allowing USER) come first and the broad rule (PUT/PATCH/DELETE → ADMIN/AGENT) comes last.

**Role management endpoint.** ✅ *Done.*
New users default to `Role.USER` and there was no way to change that. Added an admin-only `PATCH /api/users/{userId}/role` (with a `RoleRequestDto`) backed by `updateUserRole` in the service. Secured via a `requestMatchers(PATCH, "/api/users/*/role").hasRole("ADMIN")` rule placed before the catch-all so it can't be shadowed.

**Request validation.** ✅ *Done.*
`spring-boot-starter-validation` was on the classpath but unused. Added `@Valid` to every controller `@RequestBody` and constraints (`@NotBlank`, `@Email`, `@Size`, `@NotNull`) across the request DTOs, with the existing `MethodArgumentNotValidException` handler returning field-level 400s. Split shared DTOs so PATCH partial updates aren't rejected: `ProfileUpdateDto` / `TicketUpdateDto` carry null-tolerant constraints while the create DTOs stay strict.

**Block disabled users from authenticating.** ✅ *Done.*
`deleteuser` sets `isDisabled = true` (soft delete), but login previously ignored the flag. Reworked `login` to delegate to Spring's `AuthenticationManager` / `DaoAuthenticationProvider`, which runs `UserPrincipal.isEnabled()` (wired to `!isDisabled`) and throws `DisabledException` for disabled accounts and `BadCredentialsException` for bad passwords. This also removed the manual password check and a duplicate DB lookup (identity now read off the returned `Authentication` principal). Added `@ExceptionHandler`s mapping `BadCredentialsException` → 401 and `DisabledException` → 403, and updated the `UserServiceImpl` unit tests. *(Note: a JWT issued before disabling stays valid until expiry — add an `isEnabled()` guard in `JwtAuthFilter` if immediate lockout is needed.)*

**Password management.** ✅ *Done.*
Only register and login existed. Added:
- ✅ *Change password* (`PATCH /api/users/password`): authenticated, identity from the JWT principal, verifies the current password before setting the new one.
- ✅ *Forgot password* (`POST /api/users/auth/forgot-password`): looks up the user by email, persists a single-use `PasswordResetToken` (15-min expiry), and "sends" it by logging the token (stub — swap the log line for `JavaMailSender` when SMTP is set up). Always returns 200 to avoid leaking which emails are registered.
- ✅ *Reset password* (`POST /api/users/auth/reset-password`): validates the token (exists / not used / not expired) with a single generic 400 message, encodes the new password, and marks the token used (single-use). Shared `setEncodedPassword` helper keeps hashing consistent across all three flows.

Deferred (see code-quality notes): replace the `PasswordResetToken` setters with a factory.

**Consistent delete semantics.** ✅ *Done.*
Previously users soft-deleted (via `isDisabled`) while tickets and comments hard-deleted. Standardized on soft delete using Hibernate's `@SoftDelete` on all three entities (`User`, `Ticket`, `Comment`) — `delete()`/`deleteById()` now issues an `UPDATE ... SET deleted = true` and every query auto-filters deleted rows. `Profile` is left untouched (it's an `@Embeddable`, not an entity). `deleteuser` now performs a real soft delete instead of setting `isDisabled`.

Along the way, `isDisabled` was repurposed into a distinct **suspend/ban** feature (separate from deletion): an admin can toggle it, and the existing login enforcement (`UserPrincipal.isEnabled()` → `DisabledException` → 403) now backs a real capability rather than dead code.

*Known caveats (see code-quality notes): soft delete doesn't release the `unique` constraints on `username`/`email` (a deleted user's values stay reserved), and `@SoftDelete` doesn't cascade — `Ticket` keeps `cascade = REMOVE` so a deleted ticket also soft-deletes its comments.*

---

## Phase 2 — Core ticketing capabilities (next)

The features that make the system genuinely useful for day-to-day IT support.

**Richer status lifecycle.** ✅ *Done.*
Expanded `TicketStatus` to `NEW, IN_PROGRESS, ON_HOLD, RESOLVED, REOPENED, CLOSED`, with a `canTransitionTo(...)` state-machine on the enum defining the allowed moves (resolve-then-confirm, reopen from CLOSED/RESOLVED, no self-transitions). New tickets default to `NEW` and `status` was removed from the create/update DTOs, so status changes only through a dedicated, validated path: `PATCH /api/tickets/{id}/status` (with `TicketStatusUpdateDto`) rejects illegal transitions with a 400. Reopen is just a transition to `REOPENED` via that endpoint. Covered by enum transition tests and service tests (valid + illegal). *(Reopen-by-requester stays agent-gated for now — revisit with ownership-based authorization.)*

**Ticket history / audit trail.**
Add a timeline entity recording status changes, reassignments, and field edits (who, what, when). Nothing captures this today, yet `createdAt` / `updatedAt` show the data model is close.

**Attachments.**
File and screenshot uploads on tickets and comments — close to essential for IT support and currently absent.

**Internal vs. public comments.** ✅ *Done.*
Added an `internal` boolean to `Comment` (default public). Only staff can create internal comments — `addCommentToTicket` forces `internal = actor.isStaff() && request.internal()`, so a USER's `internal=true` is ignored. Reads filter it out for non-staff: `getCommentsForTicket` and `searchComments` branch on `isStaff()` (staff → all; requester → `...InternalFalse` variants), and both also gate ticket ownership via `assertCanView` (a USER only sees comments on their own tickets, 403 otherwise). Exposed on `CommentResponseDto`. Covered by internal-filter, view-gate, and create-guard tests. ✅ *Follow-up closed:* the comments **embedded in `TicketResponseDto`** (the ticket mapper serializes the full list) now also respect the filter — `TicketServiceImpl` strips internal comments for non-staff across every ticket read path (get/by-status/by-priority/search/advancedSearch/by-requester/assigned), so the leak on `GET /api/tickets/{id}` and friends is closed.

**Ownership-based authorization.** ✅ *Done.*
Security was role-based only. Layered in ownership checks where "who owns it" matters — method-level logic (service-layer guards throwing `ForbiddenException` → 403), not just `requestMatchers` rules:
- *Tickets:* ✅ Read ownership enforced — a USER sees only tickets they requested (`getAllTickets` filters; `getTicketById`/`getTicketsByRequester`/`getTicketsAssignedToUser` guard via `assertCanView`/`assertSelfOrStaff`), staff see all, 403 otherwise. Modify (edit/delete/status/assign) intentionally stays admin + any agent, already enforced by the URL rules.
- *Status/priority/search endpoints:* ✅ Gap closed — `getAllTicketsByStatus`, `getAllTicketsByPriority`, `searchTickets`, and `advancedSearch` now scope results to the requester for non-staff. Filtering is pushed into the repository queries via a nullable `requesterId` param (`null` = staff, unscoped) so pagination totals stay correct; the service computes the scope from the JWT principal (`requesterScope(actor)`). Covered by staff (unscoped) + USER (scoped) service tests.
- *User profiles:* ✅ Edit ownership enforced — `updateUser` guards via `assertSelfOrAdmin` (a USER edits only their own profile; ADMIN edits anyone), 403 otherwise. Viewing (`getUser`) intentionally left open to any authenticated user (name/email only). Covered by self/other/admin service tests.
- *Comments:* ✅ Authors can edit their own comment within a 15-minute window (staff bypass the window); delete stays staff-only.

Depended on Phase 1 (identity from JWT).

**Self-assignment & bulk actions.** ✅ *Done (bulk).*
Added **bulk status change** (`PUT /api/tickets/bulk/status`) and **bulk assign** (`PATCH /api/tickets/bulk/assign`), both with **partial-success** semantics: per-item try/catch returning `BulkResultDto(succeeded, failed)`, deliberately *not* wrapped in a single `@Transactional` (each `saveAndFlush` auto-commits so one failure doesn't roll back the batch). Bulk status reuses the `canTransitionTo` guard and reports both bad-id and illegal-transition failures; bulk assign validates the assignee once (missing assignee → whole-request 404) and reports per-item bad-ids. Staff-only via existing URL rules. Covered by all-succeed / partial / missing-assignee service tests + controller wiring tests.

*Deferred:* explicit **assign-to-me** was dropped (an agent can already self-assign via the existing `PUT /{id}/assign/{userId}` with their own id — no new capability). ✅ **Unassign** now added — `PATCH /api/tickets/{id}/unassign` sets `assignedTo = null` (staff-only via the existing URL rules); covered by service + controller tests.

**Comment search & pagination.** ✅ *Done.*
Brought comments to parity with tickets: `GET /api/comments/ticket/{ticketId}` is now paginated (`Page` + `page`/`size`/`sortBy` params, PagedModel `$.content`/`$.page` shape) and `GET /api/comments/ticket/{ticketId}/search` does a case-insensitive keyword search over `content`, scoped to one ticket (repo-level JPQL, correct pagination totals). Both are ownership-guarded: staff or the ticket's requester, 403 otherwise — this also closed a pre-existing gap where **any** USER could read the comments on **any** ticket. Covered by staff/owner/forbidden/missing service tests and controller tests (param forwarding, PagedModel shape, 403). *Open niggles: default sort is `createdAt` descending (consider ascending for conversation order); minor cleanups (leading slashes on the new mappings, orphaned non-paginated `findAllByTicketId`, unused imports).*

---

## Phase 3 — Collaboration, notifications & SLAs (later)

Features that improve responsiveness and team coordination once the core is solid.

**Notifications.**
Email and/or in-app notifications on assignment, status change, and new comments. No notification mechanism exists at all; this is the biggest experience gap after the core features.

**SLA tracking & due dates.**
Add SLA targets per priority and a `dueDate`, then flag overdue tickets. Resolution time is computable from existing timestamps but is never derived.

**Watchers / CC and @mentions.**
Let people beyond the requester and assignee follow a ticket and be notified. Builds on the notifications work.

**Managed categories & tags.**
`category` is free-text on `Ticket`. Promote it to a managed entity (or add tags) for consistent filtering and reporting.

**Canned responses / templates.**
Reusable reply snippets for common issues.

---

## Phase 4 — Insight & platform maturity (later)

Reporting and operational hardening for scale and stakeholder visibility.

**Analytics & dashboards.**
Endpoints for counts by status/priority, average resolution time, agent workload, and open-vs-closed trends. The underlying data already exists.

**Token lifecycle.**
JWTs are stateless with a 24h expiry and no refresh or revocation. Add refresh tokens, logout, and a revocation/blacklist mechanism.

**API documentation.**
Add OpenAPI / Swagger for discoverable, testable endpoints.

**Observability.**
Structured logging (noted in a `CommentController` comment) and Spring Boot Actuator health/metrics endpoints.

**Rate limiting.**
Throttle auth endpoints to resist brute-force attempts.

**Knowledge base.**
FAQ / help articles so common questions can be deflected before they become tickets.

**CI/CD & deployment hardening.**
Stand up automated build/test/deploy and make the app deployable outside localhost:
- *CI test gate:* ✅ *Done* — `.github/workflows/ci.yml` runs `./mvnw verify` (compile + JUnit tests) on every push/PR. The `@SpringBootTest` context test uses **Testcontainers** (`PostgreSQLContainer` + `@ServiceConnection`), so the test owns its Postgres and local/CI behave identically — no service block or datasource config in the workflow (dev/CI parity gap closed). *Note: running the full suite locally now requires Docker. Future: if more `@SpringBootTest` classes are added, move the container to a shared abstract base class to reuse one instance.*
- *Externalize configuration:* move the hardcoded datasource (`localhost:5433`) and the committed `jwt.secret` out of `application.properties` into environment variables / a secrets store before any real deploy.
- *Database migrations:* replace `ddl-auto=create-drop` (which wipes the schema on every startup) with Flyway or Liquibase for durable, versioned schema changes.
- *Containerize:* add a `Dockerfile` and a Postgres service (compose for local, managed DB for deployed environments).
- *CD stage:* build the image and deploy on a tagged release or merge to main. (See existing `DEPLOYMENT.md`.)

---

## Suggested sequencing summary

| Phase | Theme | Why now |
|-------|-------|---------|
| 1 | Foundations & security | Fixes trust/integrity gaps; unblocks later phases |
| 2 | Core ticketing | Makes the product genuinely usable |
| 3 | Collaboration, notifications, SLAs | Improves responsiveness once core is solid |
| 4 | Insight & platform maturity | Reporting and operational hardening for scale |

---

## Deferred refactors & code-quality notes

Small, non-blocking cleanups to revisit during a dedicated code-revision pass:

- ✅ **`PasswordResetToken` construction.** *Done.* Added `PasswordResetToken.issueFor(user, Duration)` — sets `token`/`user`/`expiresAt` directly, so tokens are valid-by-construction for the `nullable = false` columns. `forgotPassword` now calls the factory instead of raw setters. (Kept `@NoArgsConstructor` for JPA and `setUsed` for the reset-consumption path.)

- **Duplicated `assertCanView`.** The ticket view-ownership check now lives in both `TicketServiceImpl` and `CommentServiceImpl`. Two copies is tolerable; extract a shared helper (e.g. a `TicketAccessPolicy` component) if a third copy appears.

- **Comment endpoint cleanups.** Default comment sort is `createdAt` descending — consider ascending (conversation order). Also: add leading slashes to the new `ticket/...` mappings for consistency, delete the commented-out pre-pagination `getCommentsForTicket`, remove the now-unused `List<Comment> findAllByTicketId(Long)` overload and stale `List` imports.

*Roadmap generated from a review of the current codebase.*
