# ServiceLink — Feature Roadmap

A prioritized plan for evolving the ServiceLink IT ticketing backend (Spring Boot 3.4, Java 17, PostgreSQL, JWT auth). Items are grouped into four phases by impact, risk, and dependency order. Earlier phases unblock later ones — notably, deriving identity from the JWT (Phase 1) is a prerequisite for trustworthy ownership rules, notifications, and analytics.

Each item notes the relevant code so work can start quickly.

---

## Phase 1 — Foundations & security correctness (now)

These close gaps that affect data integrity and trust. They are relatively small but high-impact, and several later features depend on them.

**Derive the acting user from the JWT, not request params.** ✅ *Done.*
`createTicket` and `addComment` previously took `requesterId` / `authorId` as query params, so a caller could act as any user. Introduced a `UserPrincipal` (implements `UserDetails`, carries the `UUID`), returned it from `UserDetailsServiceImpl`, and the controllers now read the actor via `@AuthenticationPrincipal`. Controller tests updated to seed the principal into the security context. This is a prerequisite for ownership rules and notifications. *(Assignment endpoints still take an explicit `userId` — that refers to the assignee, not the actor, which is correct.)*

**Fix `SecurityConfig` rule ordering.** ✅ *Done.*
Rules are evaluated top-to-bottom (first match wins), and the broad `/api/tickets/**` and `/api/comments/**` rules (matching all HTTP methods, ADMIN/AGENT only) sat *before* the method-specific rules that included USER — so the USER-inclusive POST/GET rules were dead code and a USER got 403 creating a ticket. Reordered so the method-specific GET/POST rules (allowing USER) come first and the broad rule (PUT/PATCH/DELETE → ADMIN/AGENT) comes last.

**Role management endpoint.**
New users always default to `Role.USER` (`User` entity) and there is no API to promote anyone to `AGENT` or `ADMIN`. Add an admin-only endpoint to view and change roles.

**Request validation.**
`spring-boot-starter-validation` is already a dependency but unused. Add `@Valid` on controller bodies and constraints (`@NotBlank`, `@Email`, `@Size`) on the request DTOs.

**Block disabled users from authenticating.** ✅ *Done.*
`deleteuser` sets `isDisabled = true` (soft delete), but login previously ignored the flag. Reworked `login` to delegate to Spring's `AuthenticationManager` / `DaoAuthenticationProvider`, which runs `UserPrincipal.isEnabled()` (wired to `!isDisabled`) and throws `DisabledException` for disabled accounts and `BadCredentialsException` for bad passwords. This also removed the manual password check and a duplicate DB lookup (identity now read off the returned `Authentication` principal). Added `@ExceptionHandler`s mapping `BadCredentialsException` → 401 and `DisabledException` → 403, and updated the `UserServiceImpl` unit tests. *(Note: a JWT issued before disabling stays valid until expiry — add an `isEnabled()` guard in `JwtAuthFilter` if immediate lockout is needed.)*

**Password management.**
Only register and login exist. Add change-password (authenticated) and a forgot/reset-password flow.

**Consistent delete semantics.**
Users soft-delete while tickets hard-delete (`deleteTicketById`). Decide on one model; for an audit-sensitive support tool, soft-delete (archive) for tickets is usually preferable.

---

## Phase 2 — Core ticketing capabilities (next)

The features that make the system genuinely useful for day-to-day IT support.

**Richer status lifecycle.**
`TicketStatus` is only `NEW, IN_PROGRESS, CLOSED`. Add `RESOLVED`, `ON_HOLD`, and `REOPENED`, plus a reopen endpoint, to support resolve-then-confirm workflows.

**Ticket history / audit trail.**
Add a timeline entity recording status changes, reassignments, and field edits (who, what, when). Nothing captures this today, yet `createdAt` / `updatedAt` show the data model is close.

**Attachments.**
File and screenshot uploads on tickets and comments — close to essential for IT support and currently absent.

**Internal vs. public comments.**
Add a visibility flag so agents can leave notes the requester cannot see. (The `Comment` entity is the natural home.)

**Ownership-based authorization.**
Security is role-based only; any `AGENT` can edit or delete any ticket. Layer in ownership checks so requesters see their own tickets and agents act on assigned ones. Depends on Phase 1 (identity from JWT).

**Self-assignment & bulk actions.**
Assign-to-me, unassign, and bulk status/assignment changes to speed up queue management.

**Comment search & pagination.**
Both are already stubbed out in `CommentController` comments. Tickets already support paginated search (`searchByKeyword`, `advancedSearch`); bring comments to parity.

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

---

## Suggested sequencing summary

| Phase | Theme | Why now |
|-------|-------|---------|
| 1 | Foundations & security | Fixes trust/integrity gaps; unblocks later phases |
| 2 | Core ticketing | Makes the product genuinely usable |
| 3 | Collaboration, notifications, SLAs | Improves responsiveness once core is solid |
| 4 | Insight & platform maturity | Reporting and operational hardening for scale |

*Roadmap generated from a review of the current codebase. No application code was modified.*
