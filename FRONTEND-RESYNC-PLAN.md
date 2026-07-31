# Frontend resync — reconnect the Angular app to the current backend API

**Status:** steps 1–2 (models, services) **done**; steps 3–6 open. Written 2026-07-29 against `main` @ `23ecb06`; revised 2026-07-30 against `main` @ `635a203` (after step 2 merged as PR #9).

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

## Already done (steps 1–2)

### Step 1 — models (commit `7a3455e`, PR #8)

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

### Step 2 — services (commit `91d1fb9`, PR #9)

- **`core/services/ticket.service.ts`** — `assignTicket` `PATCH` → **`PUT`**; `createTicket` dropped the `requesterId` arg + query param; added `updateTicketStatus(id, status)` → `PATCH /{id}/status` with body `{ ticketStatus }`.
- **`core/services/comment.service.ts`** — `updateComment` `PATCH` → **`PUT`**; `addComment` dropped the `authorId` arg + query param; `getCommentsForTicket` retyped to `Observable<PageResponse<CommentResponse>>` and now takes `page`/`size`.
- **Call sites updated:** `ticket-form.ts` (`createTicket`), `ticket-detail.ts` (`addComment`).

**Two corrections were needed on top** — both generic-nesting traps worth recording:

1. `getCommentsForTicket` was first typed `PageResponse<CommentResponse[]>`. `PageResponse<T>` already declares `content: T[]`, so `T` is the **element** type — passing the array type yields `content: CommentResponse[][]`. A follow-up attempt then dropped the `Observable<>` wrapper from the signature, producing *"Type `Observable<…>` is missing the following properties … `content`, `page`"*. The correct form is `Observable<PageResponse<CommentResponse>>` on **both** the return type and the `http.get<…>` generic: the generic describes the JSON body the server sends, and the return type is that same thing wrapped in `Observable`.
2. `addComment` kept a dead `authorId` **parameter** after the `?authorId=` query string was removed — it still compiled, and `ticket-detail.ts` went on passing an argument into the void. Removing the parameter is what surfaces the call site.

---

## What's actually broken

Verified against `TicketController`, `CommentController`, `UserController`, `SecurityConfig`, and the DTO records. Line numbers were accurate as of `23ecb06` and have drifted since — treat them as hints, not addresses. Items marked ✅ are resolved by the committed work above.

### Runtime failures

| # | Where | Problem |
|---|-------|---------|
| 1 | `core/services/ticket.service.ts` | ✅ **fixed** (step 2). `assignTicket` sent `PATCH /{id}/assign/{userId}`; backend maps **`PUT`** → 405 |
| 2 | `core/services/comment.service.ts` | ✅ **fixed** (step 2). `updateComment` sent `PATCH /{commentId}`; backend maps **`PUT`** → 405. `SecurityConfig` also only lets a USER through on `PUT /api/comments/**`; PATCH fell to the ADMIN/AGENT rule |
| 3 | `core/services/comment.service.ts` | ✅ **fixed** (step 2). `getCommentsForTicket` was typed `CommentResponse[]`, but the endpoint returns a `PagedModel` object — iterating it broke |
| 4 | `models/ticket.model.ts:25` | ✅ **fixed.** `PageResponse<T>` expected flat metadata (`totalElements`, `number`, `first`…); `VIA_DTO` nests it under `$.page`, so everything except `content` read `undefined` |
| 5 | `models/ticket.model.ts:34` | ✅ **fixed** (enum values). `Status` had 3 of the backend's 6. Still open downstream: dashboard counts, the filter dropdown, and the badge styling in step 3 |
| 6 | `core/pages/tickets/ticket-detail/` | **Still open — no way to change status.** Step 2 added `updateTicketStatus` to `ticket.service.ts`, but **nothing calls it** (`grep` finds only the definition), so the workflow the state machine exists to serve is still unreachable. One component away rather than absent — this is step 3's headline item |
| 7 | `core/pages/tickets/ticket-detail/ticket-detail.ts:62` | Calls `getUserById(ticket.assignedTo)` unconditionally; an unassigned ticket sends `assignedTo: null` → request to `/api/users/null` |
| 8 | `models/user.model.ts:20` | ✅ **fixed.** `Role` was `'Admin'`/`'Agent'`/`'User'`; backend serializes `ADMIN`/`AGENT`/`USER`. Was latent — `sidebar.html:18` compares the literal `'ADMIN'` and worked — but `roleGuard` would break the moment anyone passed `Role.ADMIN` |

### Vestigial — silently ignored, remove

Harmless only because the backend sets `spring.jackson.deserialization.fail-on-unknown-properties=false` and Spring ignores unknown query params. Misleading to read.

9. ✅ **fixed** (step 2). `createTicket` appended `?requesterId=`; `addComment` appended `?authorId=`. Identity comes from the JWT principal — both were dead, and both the params and the arguments are now gone.
10. ✅ **fixed.** `TicketRequest.status` and the status `<select>` in `ticket-form.html`. `TicketRequestDto` has no `status` field; new tickets are always `NEW`.

### Model gaps

11. ✅ **fixed.** `CommentResponse.authorId` was `number`; it's a UUID string. Added the missing `internal: boolean`.
12. ✅ **fixed.** `UserResponse` was an empty interface `{}`; now mirrors `UserResponseDto`.

### Hygiene

13. `core/interceptors/jwt.interceptor.ts:9-14` logs **the bearer token** to the console, plus four more debug lines. Debug `console.log`s also in `dashboard.ts`, `login.ts`, `ticket-detail.ts`.
14. Every `.spec.ts` is a CLI stub calling `TestBed.configureTestingModule({})` with no `provideHttpClient`, so any service injecting `HttpClient` fails to construct. The suite is almost certainly red — confirm in step 0 and don't attribute those failures to this work.

---

## Found while implementing (2026-07-30)

Four things the original inventory missed. Recorded so the next session doesn't re-derive them.

15. **`Ticket.comments` has no `@OrderBy`** — verified, there is no `@OrderBy` *anywhere* in the backend. `ticket-detail` iterates `ticket.comments` (serialized by the ticket mapper), so its order is whatever Postgres returns. PR #7's oldest-first guarantee applies **only** to `GET /api/comments/ticket/{id}`, which the frontend never calls. **Consequence: the verification bullet below about comments reading oldest-first cannot pass as the code stands.** Two ways to fix it — switch `ticket-detail` to the comment endpoint (frontend, step 3; also puts the retyped service method into real use), or add `@OrderBy("createdAt ASC")` to `Ticket.comments` (backend, one line). Latent rather than visible: a simple table scan usually returns insertion order today, so it probably *looks* correct.

16. **The status state machine is restrictive and has no self-transitions** — from `TicketStatus.canTransitionTo`:

    | From | Allowed targets |
    |------|-----------------|
    | `NEW` | `IN_PROGRESS` |
    | `IN_PROGRESS` | `ON_HOLD`, `RESOLVED` |
    | `ON_HOLD` | `IN_PROGRESS` |
    | `RESOLVED` | `CLOSED`, `REOPENED` |
    | `CLOSED` | `REOPENED` |
    | `REOPENED` | `IN_PROGRESS` |

    A dropdown listing all six values would 400 on most selections. Step 3 needs a `Record<Status, Status[]>` map mirroring this, kept beside the `Status` enum under the same "mirrors backend" comment convention the enums already use.

17. **The status-badge `NgClass` block is triplicated** across `dashboard.html`, `ticket-list.html`, and `ticket-detail.html`, each covering 3 of 6 statuses. Worth collapsing into one exported `Record<Status, string>` rather than extending the same block in three places. **Caveat:** Tailwind 4 detects classes by scanning source files, so the map's values must be complete literal class strings — never interpolated like `` `text-${color}-400` `` — or the utilities won't be generated.

18. **Two pre-existing `NG8113` build warnings**, unrelated to this resync: `App` declares `Login`, and `TicketList` declares `TicketForm`, in their `imports` arrays without using them in their templates. Harmless; trivial cleanup if you want quiet build output.

---

## Plan

### 0. Get the toolchain running ✅ DONE (locally)

`node_modules` is installed and **`npm run build` is clean** as of `635a203` (two pre-existing `NG8113` warnings — see item 18). On a fresh machine this still starts with `npm install`.

⚠️ **The `npm test` baseline was never captured.** Do that before touching the specs in step 5, so the stub-spec failures (item 14) aren't misread as regressions introduced by this work.

### 1. Models — `src/app/models/` ✅ DONE

See *Already done → step 1* above. The models now match the backend DTOs; everything below builds on that.

### 2. Services — `src/app/core/services/` ✅ DONE

See *Already done → step 2* above, including the two generic-nesting corrections. Note the status body key is **`ticketStatus`**, matching `TicketStatusUpdateDto`'s record component — a mismatch fails validation with a 400.

### 3. Components ⬅️ NEXT

- **`models/ticket.model.ts`**: add the `Record<Status, Status[]>` transition map from item 16, beside the `Status` enum.
- **`ticket-detail.ts`**: guard the `assignedTo` lookup — `if (ticket.assignedTo) { … }`, else leave `assignedToProfile` null; the template already renders "Unassigned". Then add the **status-change control**: a `<select>` driven by the transition map for the ticket's current status, calling `updateTicketStatus` and refreshing the ticket on success.
  - Show it **only for staff** (`ADMIN`/`AGENT`). `SecurityConfig` restricts `PATCH /api/tickets/**` to ADMIN/AGENT, so a USER gets a 403.
  - An **illegal transition returns 400** with a message from `canTransitionTo` — surface it instead of failing silently. Keep this handler even with the transition map in place: it's the safety net if the client mirror ever drifts from the backend enum.
  - Render nothing (or a disabled control) when the allowed list is empty.
  - Optionally switch the comment list from `ticket.comments` to `getCommentsForTicket` — see item 15.
- **`dashboard.ts`**: `openTickets` counts only `NEW`; with six statuses it should count everything non-terminal — `NEW`, `IN_PROGRESS`, `ON_HOLD`, `REOPENED`. **`ticket-list.ts`** needs no change: `statuses = Object.values(Status)` already picks up all six.
- **Badges** in `dashboard.html`, `ticket-list.html`, `ticket-detail.html`: each `NgClass` block covers 3 of 6 statuses. Prefer the shared map in item 17 over extending the same block three times. (They compare string literals, not the enum, so they don't break at compile time — they just render unstyled.)

*Already done in step 1:* `shared/components/ticket-form/` — the status `<select>`, `status` field, and `statuses` list are gone; the `requesterId` argument went with step 2.

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
   - As ADMIN/AGENT: walk `NEW → IN_PROGRESS → RESOLVED → CLOSED → REOPENED` via the new control, confirming the dropdown offers **only legal moves** at each step (item 16). Force one illegal transition (e.g. via devtools) to confirm the 400 surfaces. Assign a ticket (confirms `PUT`); open an **unassigned** ticket and confirm no `/api/users/null` in the network tab.
   - Add a comment, then edit it (confirms `PUT`). **Comments read oldest-first only if `ticket-detail` was switched to the comment endpoint** — `ticket.comments` has no guaranteed order (item 15). Skip this check if that change wasn't made; don't read a passing result as proof the ordering is pinned.
   - As a USER: only your own tickets are visible, and the status control is hidden.
4. Backend is untouched, so `cd Backend && ./mvnw verify` should be unaffected — worth one run to confirm. Note `src/main/resources/application.properties` is **gitignored**, so it won't exist on a fresh clone; recreate it for local runs (dev datasource + `jwt.secret`).

**Branches:** step 1 → PR #8 (`7a3455e`); step 2 → `fix/frontend-api-resync`, PR #9 (`91d1fb9`, merged). Step 3 gets a fresh branch off `main`; conventional-commit messages (`fix(frontend): …`).

---

## Deferred to follow-up work

No UI yet for: **ticket history timeline** (`GET /api/tickets/{id}/history`), **unassign**, **bulk status/assign**, **advanced search**, **comment search + pagination controls**, the **internal-comment toggle**, **password change/forgot/reset**, **role management**, **suspend/ban**, and **user registration**. Each is additive once the contract is correct.

Also unaddressed: `TicketResponseDto` returns `assignedTo`/`requester` as bare UUIDs, so any view without a follow-up `getUserById` shows a raw UUID. `ticket-detail` resolves them; list views don't. Fixing it properly means adding display names to the DTO — a backend change worth its own decision.

Two backend changes worth considering later, both surfaced by items 15–16:

- **Expose allowed transitions from the backend** (e.g. on `TicketResponseDto`, or a `GET /api/tickets/{id}/transitions`). The step-3 transition map duplicates `TicketStatus.canTransitionTo` on the client, which is exactly the kind of drift this whole resync exists to clean up. A server-provided list removes the duplicate.
- **`@OrderBy("createdAt ASC")` on `Ticket.comments`** — the one-line alternative to routing `ticket-detail` through the comment endpoint, and it fixes the ordering for every consumer of the embedded list rather than just this one view.

**After this:** attachments — the last Phase 2 item.
