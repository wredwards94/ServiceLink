# Frontend resync — reconnect the Angular app to the current backend API

**Status:** step 1 (models) largely **done**; steps 2–6 open. Written 2026-07-29, against `main` @ `23ecb06` (after `chore/comment-endpoint-cleanups` merged as PR #7).

**Handoff doc.** Chat history doesn't sync between machines — this file, `session-context.md`, `Backend/ROADMAP.md`, and the git history are the durable record (see `session-context.md` §9).

---

## How to resume on another machine

```bash
git pull
cd Frontend/ServiceLink
npm install          # node_modules is NOT committed and starts empty
npm run build        # capture the baseline before changing anything
npm test
```

Then work through the steps below in order, starting at **step 0**.

Docker is **not** needed for frontend work — only the two backend Testcontainers tests (`TicketHistoryTest`, `ServiceLinkApplicationTests`) require it, and they skip cleanly without it via `@EnabledIf`.

To point a fresh Claude Code session at the right context: `session-context.md` + `Backend/ROADMAP.md` + this file.

---

## Context

`Frontend/ServiceLink/` is a real Angular 21 app (Tailwind 4, vitest) — login, dashboard, ticket list/detail, ticket form, sidebar, auth/role guards, a JWT interceptor, and services for tickets/comments/users. It is **not** scaffolding.

But it was written against the *pre-Phase-1* API and never caught up. Phase 1 moved identity into the JWT; Phase 2 expanded the status lifecycle to six states, moved status changes to a dedicated endpoint, and paginated the comment reads. The frontend knows about none of it.

Result: several core paths fail at runtime, and **a ticket's status cannot be changed from the UI at all**.

This lands before **attachments** (the last Phase 2 item) because attachments need both halves anyway — better to build them once, against a frontend that works. It's also the first thing that will exercise these endpoints end-to-end: the backend suite mocks or slices at every layer, and per `session-context.md` §3, three separate `@Query` bugs were caught only by the full context test. A working UI is a better contract test than another Mockito suite.

**Scope: make the existing app correct — not add new surfaces.** Missing features are listed under *Deferred* at the bottom.

---

## Already done (step 1 — models)

These were in the working tree uncommitted and are now committed, so they travel with the repo:

- **`comment.model.ts`** — `authorId: string` (was `number`), added `internal: boolean`.
- **`ticket.model.ts`** — dropped `status` from `TicketRequest`; reshaped `PageResponse<T>` to the nested `VIA_DTO` form.
- **`user.model.ts`** — filled in `UserResponse` from `UserResponseDto`; `Role` corrected to `'ADMIN'`/`'AGENT'`/`'USER'`.
- **`ticket-form`** — status `<select>` removed from the template, `status` removed from the request object, and the now-dead `statuses` list + `Status` import cleaned up.
- Housekeeping: `angular.json` `analytics: false`, `@angular/cli` pinned to `^21.0.4`, and `dashboard.spec.ts`'s import fixed from default to named (`import { Dashboard }`).

**Two corrections were needed on top:**

1. `ON_HOLD` / `RESOLVED` / `REOPENED` had been added to the **`Priority`** enum instead of **`Status`**, and `ON_HOLD` was spelled `'ON HOLD'` (space). `Priority` is exactly `LOW`/`MEDIUM`/`HIGH`/`CRITICAL` per `TicketPriority.java` — the extra values would have offered bogus options in the priority filter and 400'd on `GET /api/tickets/priority/{priority}`. Moved to `Status` with the correct underscore value; both enums now carry a comment naming the backend enum they mirror.
2. `ticket-form.ts` still declared `statuses = Object.values(Status)` after the template's `<select>` was removed — removed along with the unused import.

**Lesson for the handoff:** that work sat uncommitted, so it would have been lost moving machines. Commit before switching.

---

## What's actually broken

Verified against `TicketController`, `CommentController`, `UserController`, `SecurityConfig`, and the DTO records. Line numbers are as of `23ecb06`; items marked ✅ are resolved by the committed model work above.

### Runtime failures

| # | Where | Problem |
|---|-------|---------|
| 1 | `core/services/ticket.service.ts:56` | `assignTicket` sends `PATCH /{id}/assign/{userId}`; backend maps **`PUT`** → 405 |
| 2 | `core/services/comment.service.ts:35` | `updateComment` sends `PATCH /{commentId}`; backend maps **`PUT`** → 405. `SecurityConfig` also only lets a USER through on `PUT /api/comments/**`; PATCH falls to the ADMIN/AGENT rule |
| 3 | `core/services/comment.service.ts:27` | `getCommentsForTicket` typed `CommentResponse[]`, but the endpoint returns a `PagedModel` object — iterating it breaks |
| 4 | `models/ticket.model.ts:25` | ✅ **fixed.** `PageResponse<T>` expected flat metadata (`totalElements`, `number`, `first`…); `VIA_DTO` nests it under `$.page`, so everything except `content` read `undefined` |
| 5 | `models/ticket.model.ts:34` | ✅ **fixed** (enum values). `Status` had 3 of the backend's 6. Still open downstream: dashboard counts, the filter dropdown, and the badge styling in step 3 |
| 6 | — | **No way to change status.** `PATCH /api/tickets/{id}/status` is never called, and `status` was removed from the update DTO, so the workflow the state machine exists to serve is unreachable |
| 7 | `core/pages/tickets/ticket-detail/ticket-detail.ts:62` | Calls `getUserById(ticket.assignedTo)` unconditionally; an unassigned ticket sends `assignedTo: null` → request to `/api/users/null` |
| 8 | `models/user.model.ts:20` | ✅ **fixed.** `Role` was `'Admin'`/`'Agent'`/`'User'`; backend serializes `ADMIN`/`AGENT`/`USER`. Was latent — `sidebar.html:18` compares the literal `'ADMIN'` and worked — but `roleGuard` would break the moment anyone passed `Role.ADMIN` |

### Vestigial — silently ignored, remove

Harmless only because the backend sets `spring.jackson.deserialization.fail-on-unknown-properties=false` and Spring ignores unknown query params. Misleading to read.

9. `createTicket` appends `?requesterId=`; `addComment` appends `?authorId=`. Identity comes from the JWT principal — both are dead. **Still open** (step 2).
10. ✅ **fixed.** `TicketRequest.status` and the status `<select>` in `ticket-form.html`. `TicketRequestDto` has no `status` field; new tickets are always `NEW`.

### Model gaps

11. ✅ **fixed.** `CommentResponse.authorId` was `number`; it's a UUID string. Added the missing `internal: boolean`.
12. ✅ **fixed.** `UserResponse` was an empty interface `{}`; now mirrors `UserResponseDto`.

### Hygiene

13. `core/interceptors/jwt.interceptor.ts:9-14` logs **the bearer token** to the console, plus four more debug lines. Debug `console.log`s also in `dashboard.ts`, `login.ts`, `ticket-detail.ts`.
14. Every `.spec.ts` is a CLI stub calling `TestBed.configureTestingModule({})` with no `provideHttpClient`, so any service injecting `HttpClient` fails to construct. The suite is almost certainly red — confirm in step 0 and don't attribute those failures to this work.

---

## Plan

### 0. Get the toolchain running

Nothing is verifiable until this passes. `npm install`, then `npm run build` and `npm test` to capture the **baseline** failure count.

### 1. Models — `src/app/models/` ✅ DONE

See *Already done* above. The models now match the backend DTOs; everything below builds on that.

### 2. Services — `src/app/core/services/`

- **`ticket.service.ts`**: `assignTicket` → `PUT`. Drop the `requesterId` arg + query param from `createTicket`. Add:
  ```ts
  updateTicketStatus(id: number, status: Status): Observable<TicketResponse> {
    return this.http.patch<TicketResponse>(`${this.apiUrl}/${id}/status`, { ticketStatus: status });
  }
  ```
  The body key is **`ticketStatus`**, matching `TicketStatusUpdateDto`'s record component — easy to get wrong, and a mismatch fails validation with a 400.
- **`comment.service.ts`**: `updateComment` → `PUT`. Drop the `authorId` arg + param from `addComment`. Retype `getCommentsForTicket` to `Observable<PageResponse<CommentResponse>>` and accept `page`/`size`.

### 3. Components

- **`shared/components/ticket-form/`**: the status `<select>`, `status` field, and `statuses` list are already gone. Still to do: drop the `requesterId` argument once `createTicket`'s signature changes in step 2.
- **`ticket-detail.ts`**: guard the `assignedTo` lookup — `if (ticket.assignedTo) { … }`, else leave `assignedToProfile` null; the template at line 46 already renders "Unassigned". Then add the **status-change control**: a `<select>` over `Status` calling `updateTicketStatus`, refreshing the ticket on success.
  - Show it **only for staff** (`ADMIN`/`AGENT`). `SecurityConfig` restricts `PATCH /api/tickets/**` to ADMIN/AGENT, so a USER gets a 403.
  - An **illegal transition returns 400** with a message from `canTransitionTo` — surface it instead of failing silently. Valid moves are defined on `TicketStatus.canTransitionTo`; there are no self-transitions.
- **`dashboard.ts`** / **`ticket-list.ts`**: no signature changes, but revisit the counts now that six statuses exist — `openTickets` currently counts only `NEW`.
- **Badges** in `dashboard.html:56-58`, `ticket-list.html:71-73`, `ticket-detail.html:19-21`: each `NgClass` block covers 3 statuses. Extend to all 6, same pattern. (They compare string literals, not the enum, so they don't break at compile time — they just render unstyled.)

### 4. Hygiene

Strip the token logging and debug `console.log`s from `jwt.interceptor.ts`, `dashboard.ts`, `login.ts`, `ticket-detail.ts`. Keep `console.error` in error handlers.

### 5. Tests

Give the stub specs a working `TestBed` — `provideHttpClient()` + `provideHttpClientTesting()` — then use `HttpTestingController` to assert **method and URL**, which is exactly what would have caught this drift:

- `ticket.service.spec.ts`: `assignTicket` issues `PUT`; `updateTicketStatus` issues `PATCH` to `/{id}/status` with body `{ ticketStatus }`; `createTicket` sends **no** `requesterId` param.
- `comment.service.spec.ts`: `updateComment` issues `PUT`; `getCommentsForTicket` parses the nested `$.page` shape.

Mirrors how the backend pinned its own contract regression — `CommentControllerTest` now captures the `Pageable` and asserts `Sort.Direction.ASC`.

### 6. Docs

Add a **Frontend** section to `Backend/ROADMAP.md` (or promote it to a repo-root `ROADMAP.md` — it already covers more than the backend) recording that the app exists, what this resync fixed, and the deferred list below. Update `session-context.md` to make the frontend a tracked deliverable and note the `npm install` + Docker-free local loop.

---

## Verification

1. **`npm run build`** — clean. TypeScript should now *fail* on any remaining caller passing `requesterId`/`authorId` or reading `PageResponse.totalElements`. Treat those errors as the checklist.
2. **`npm test`** — new service specs green; overall failures no worse than the step-0 baseline.
3. **End-to-end against a live backend** — the real gate; the type system cannot catch a wrong HTTP verb.
   - Postgres on `localhost:5433` (db `postgres`, user `postgres`, pass `root`), then `cd Backend && ./mvnw spring-boot:run`, then `npm start`.
   - Log in → dashboard lists tickets, counts look right.
   - Create a ticket → lands as `NEW`.
   - As ADMIN/AGENT: change status via the new control, **including one illegal transition** to confirm the 400 surfaces; assign a ticket (confirms `PUT`); open an **unassigned** ticket and confirm no `/api/users/null` in the network tab.
   - Add a comment, then edit it (confirms `PUT`); confirm comments read **oldest-first** — that ordering changed in PR #7.
   - As a USER: only your own tickets are visible, and the status control is hidden.
4. Backend is untouched, so `cd Backend && ./mvnw verify` should be unaffected — worth one run to confirm. Note `src/main/resources/application.properties` is **gitignored**, so it won't exist on a fresh clone; recreate it for local runs (dev datasource + `jwt.secret`).

**Branch:** `fix/frontend-api-resync`, PR to `main`, conventional-commit messages (`fix(frontend): …`).

---

## Deferred to follow-up work

No UI yet for: **ticket history timeline** (`GET /api/tickets/{id}/history`), **unassign**, **bulk status/assign**, **advanced search**, **comment search + pagination controls**, the **internal-comment toggle**, **password change/forgot/reset**, **role management**, **suspend/ban**, and **user registration**. Each is additive once the contract is correct.

Also unaddressed: `TicketResponseDto` returns `assignedTo`/`requester` as bare UUIDs, so any view without a follow-up `getUserById` shows a raw UUID. `ticket-detail` resolves them; list views don't. Fixing it properly means adding display names to the DTO — a backend change worth its own decision.

**After this:** attachments — the last Phase 2 item.
