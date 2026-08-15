import { formatBytes, formatElapsed, parseApiDate } from './time';

describe('parseApiDate', () => {
  /** The backend's @JsonFormat pattern is "MM/dd/yyyy hh:mm a". */
  it('parses the backend timestamp format', () => {
    const date = parseApiDate('08/15/2026 09:30 PM');
    expect(date).not.toBeNull();
    expect(date!.getFullYear()).toBe(2026);
    expect(date!.getMonth()).toBe(7); // August, zero-indexed
    expect(date!.getDate()).toBe(15);
    expect(date!.getHours()).toBe(21);
    expect(date!.getMinutes()).toBe(30);
  });

  it('handles midnight and noon, where a naive %12 goes wrong', () => {
    expect(parseApiDate('01/01/2026 12:00 AM')!.getHours()).toBe(0);
    expect(parseApiDate('01/01/2026 12:00 PM')!.getHours()).toBe(12);
  });

  it('returns null rather than an Invalid Date', () => {
    expect(parseApiDate(null)).toBeNull();
    expect(parseApiDate('')).toBeNull();
    expect(parseApiDate('not a date')).toBeNull();
  });
});

describe('formatElapsed', () => {
  const from = new Date(2026, 7, 15, 12, 0);

  it('reports coarse dwell', () => {
    expect(formatElapsed(from, new Date(2026, 7, 15, 12, 0, 30))).toBe('just now');
    expect(formatElapsed(from, new Date(2026, 7, 15, 12, 42))).toBe('42m');
    expect(formatElapsed(from, new Date(2026, 7, 15, 15, 0))).toBe('3h');
    expect(formatElapsed(from, new Date(2026, 7, 15, 15, 20))).toBe('3h 20m');
    expect(formatElapsed(from, new Date(2026, 7, 18, 12, 0))).toBe('3d');
  });

  it('has no measurement to report without a timestamp', () => {
    expect(formatElapsed(null)).toBe('—');
  });
});

describe('formatBytes', () => {
  it('scales to a readable unit', () => {
    expect(formatBytes(512)).toBe('512 B');
    expect(formatBytes(14336)).toBe('14 KB');
    expect(formatBytes(2_411_724)).toBe('2.3 MB');
  });
});
