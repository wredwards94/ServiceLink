/**
 * The backend serialises every LocalDateTime with
 * `@JsonFormat(pattern = "MM/dd/yyyy hh:mm a")`, e.g. "08/15/2026 09:30 PM".
 *
 * `new Date(...)` happens to parse that string in V8, but the behaviour is
 * implementation-defined for non-ISO input and differs across engines — so it
 * is parsed explicitly here instead. Anything unrecognised returns null and
 * callers fall back to showing the raw string.
 */
const API_DATE = /^(\d{2})\/(\d{2})\/(\d{4})\s+(\d{2}):(\d{2})\s+(AM|PM)$/i;

export function parseApiDate(value: string | null | undefined): Date | null {
  if (!value) return null;

  const match = API_DATE.exec(value.trim());
  if (!match) {
    const fallback = new Date(value);
    return Number.isNaN(fallback.getTime()) ? null : fallback;
  }

  const [, mm, dd, yyyy, rawHour, minute, meridiem] = match;
  let hour = Number(rawHour) % 12;
  if (meridiem.toUpperCase() === 'PM') hour += 12;

  const date = new Date(
    Number(yyyy),
    Number(mm) - 1,
    Number(dd),
    hour,
    Number(minute),
  );
  return Number.isNaN(date.getTime()) ? null : date;
}

/**
 * Elapsed time, coarse to one or two units — "3d 4h", "42m", "just now".
 *
 * This measures dwell: how long a ticket has sat since it last moved. It is
 * NOT a deadline. The backend has no due-date or SLA field, so nothing in this
 * app may imply one; the label beside a dwell readout says "quiet for", never
 * "due in".
 */
export function formatElapsed(from: Date | null, now: Date = new Date()): string {
  if (!from) return '—';

  const seconds = Math.floor((now.getTime() - from.getTime()) / 1000);
  if (seconds < 0) return 'just now';
  if (seconds < 60) return 'just now';

  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m`;

  const hours = Math.floor(minutes / 60);
  if (hours < 24) {
    const restMinutes = minutes % 60;
    return restMinutes ? `${hours}h ${restMinutes}m` : `${hours}h`;
  }

  const days = Math.floor(hours / 24);
  const restHours = hours % 24;
  if (days < 30) return restHours ? `${days}d ${restHours}h` : `${days}d`;

  const months = Math.floor(days / 30);
  return `${months}mo`;
}

/** Short absolute stamp for timelines: "15 Aug, 21:30". */
export function formatStamp(value: string | null | undefined): string {
  const date = parseApiDate(value);
  if (!date) return value ?? '—';

  return new Intl.DateTimeFormat(undefined, {
    day: 'numeric',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date);
}

/** Bytes as a compact human size — "14 KB", "2.3 MB". */
export function formatBytes(bytes: number): string {
  if (!Number.isFinite(bytes) || bytes < 0) return '—';
  if (bytes < 1024) return `${bytes} B`;

  const units = ['KB', 'MB', 'GB'];
  let value = bytes / 1024;
  let unit = 0;

  while (value >= 1024 && unit < units.length - 1) {
    value /= 1024;
    unit++;
  }

  return `${value < 10 ? value.toFixed(1) : Math.round(value)} ${units[unit]}`;
}
