import { CommentResponse } from './comment.model';

export interface TicketResponse {
  id: number;
  title: string;
  description: string;
  category: string;
  status: Status;
  priority: Priority;
  assignedTo: string | null;
  requester: string;
  createdAt: string;
  updatedAt: string;
  comments: CommentResponse[];
}

export interface TicketRequest {
  title: string;
  description: string;
  category: string;
  priority: Priority;
}

/** Mirrors TicketUpdateDto — every field optional, omitted fields left unchanged. */
export interface TicketUpdate {
  title?: string;
  description?: string;
  category?: string;
  priority?: Priority;
}

export interface PageResponse<T> {
  content: T[];
  page: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}

// Mirrors backend enums/TicketStatus.java — values must match exactly.
export enum Status {
  NEW = 'NEW',
  IN_PROGRESS = 'IN_PROGRESS',
  ON_HOLD = 'ON_HOLD',
  RESOLVED = 'RESOLVED',
  REOPENED = 'REOPENED',
  CLOSED = 'CLOSED',
}

// Mirrors backend enums/TicketPriority.java.
export enum Priority {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH',
  CRITICAL = 'CRITICAL',
}

// Mirrors TicketStatus.canTransitionTo. The backend is still the authority —
// an illegal target returns 400 — but the UI never offers one.
export const ALLOWED_TRANSITIONS: Record<Status, Status[]> = {
  [Status.NEW]: [Status.IN_PROGRESS],
  [Status.IN_PROGRESS]: [Status.ON_HOLD, Status.RESOLVED],
  [Status.ON_HOLD]: [Status.IN_PROGRESS],
  [Status.RESOLVED]: [Status.CLOSED, Status.REOPENED],
  [Status.REOPENED]: [Status.IN_PROGRESS],
  [Status.CLOSED]: [Status.REOPENED],
};

/* --------------------------------------------------------------------------
   Lifecycle topology
   --------------------------------------------------------------------------
   The state machine is not a straight line, and drawing it as one would lie
   about it. It is a four-station main line with two off-line states:

        ON_HOLD                          (siding — parks work in progress)
           │
   NEW ── IN_PROGRESS ── RESOLVED ── CLOSED
           ▲                 │          │
           └──────── REOPENED ◄─────────┘   (return — sends work back)

   MAIN is what the rail draws left to right; OFF_MAIN hangs above and below it.
   -------------------------------------------------------------------------- */

export const LIFECYCLE_MAIN: Status[] = [
  Status.NEW,
  Status.IN_PROGRESS,
  Status.RESOLVED,
  Status.CLOSED,
];

/** Where each state sits along the main line, including the two off-line ones. */
export const LIFECYCLE_INDEX: Record<Status, number> = {
  [Status.NEW]: 0,
  [Status.IN_PROGRESS]: 1,
  [Status.ON_HOLD]: 1, // parked alongside IN_PROGRESS
  [Status.RESOLVED]: 2,
  [Status.CLOSED]: 3,
  [Status.REOPENED]: 1, // headed back into IN_PROGRESS
};

/** Nothing further happens to a ticket in one of these without a human acting. */
export const TERMINAL_STATUSES: Status[] = [Status.CLOSED];

/** Everything still on someone's plate — what "open" means with six states. */
export const OPEN_STATUSES: Status[] = [
  Status.NEW,
  Status.IN_PROGRESS,
  Status.ON_HOLD,
  Status.REOPENED,
];

/** Sentence-case labels. Screaming enum values are for the wire, not the eye. */
export const STATUS_LABEL: Record<Status, string> = {
  [Status.NEW]: 'New',
  [Status.IN_PROGRESS]: 'In progress',
  [Status.ON_HOLD]: 'On hold',
  [Status.RESOLVED]: 'Resolved',
  [Status.REOPENED]: 'Reopened',
  [Status.CLOSED]: 'Closed',
};

export const PRIORITY_LABEL: Record<Priority, string> = {
  [Priority.LOW]: 'Low',
  [Priority.MEDIUM]: 'Medium',
  [Priority.HIGH]: 'High',
  [Priority.CRITICAL]: 'Critical',
};

/* Class maps replace the NgClass blocks that were triplicated across the
   dashboard, list, and detail templates — each covering only three of the six
   states, so ON_HOLD / RESOLVED / REOPENED rendered unstyled.

   Values are complete literal class strings. Tailwind 4 finds classes by
   scanning source text, so an interpolated name like `state-${x}` would never
   be generated. These particular classes are hand-authored in styles.css
   rather than generated, but the literal-string rule holds regardless. */
export const STATUS_CLASS: Record<Status, string> = {
  [Status.NEW]: 'state state-live',
  [Status.IN_PROGRESS]: 'state state-live',
  [Status.ON_HOLD]: 'state state-held',
  [Status.RESOLVED]: 'state state-done',
  [Status.REOPENED]: 'state state-live',
  [Status.CLOSED]: 'state state-done',
};

export const PRIORITY_CLASS: Record<Priority, string> = {
  [Priority.LOW]: 'sev sev-low',
  [Priority.MEDIUM]: 'sev sev-medium',
  [Priority.HIGH]: 'sev sev-high',
  [Priority.CRITICAL]: 'sev sev-critical',
};
