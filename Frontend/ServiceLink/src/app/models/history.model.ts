/**
 * One entry in a ticket's audit timeline, from GET /api/tickets/{id}/history.
 * Mirrors TicketHistoryEntryDto.
 *
 * CREATED / DELETED rows carry no field detail — `field`, `oldValue`, and
 * `newValue` are null on those. MODIFIED rows describe a single field changing.
 */
export interface TicketHistoryEntry {
  revision: number;
  timestamp: string;
  actorName: string | null;
  actorId: string | null;
  type: string;
  field: string | null;
  oldValue: string | null;
  newValue: string | null;
}
