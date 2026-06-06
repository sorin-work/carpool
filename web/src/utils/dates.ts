import type { Weekday } from '@/api/types'

export const WEEKDAYS: Weekday[] = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
]

export const WEEKDAY_SHORT: Record<Weekday, string> = {
  MONDAY: 'Mon',
  TUESDAY: 'Tue',
  WEDNESDAY: 'Wed',
  THURSDAY: 'Thu',
  FRIDAY: 'Fri',
  SATURDAY: 'Sat',
  SUNDAY: 'Sun',
}

/** "Mon–Fri", "Every day" or "Mon, Wed, Fri". */
export function describeDays(days: Weekday[]): string {
  const sorted = WEEKDAYS.filter((d) => days.includes(d))
  if (sorted.length === 7) return 'Every day'
  if (sorted.length === 5 && !sorted.includes('SATURDAY') && !sorted.includes('SUNDAY')) {
    return 'Mon–Fri'
  }
  return sorted.map((d) => WEEKDAY_SHORT[d]).join(', ')
}

/**
 * A calendar date ("2026-03-03") as a local Date at midnight. `new Date("2026-03-03")` would be
 * midnight UTC, which is still the previous day anywhere west of Greenwich.
 */
export function parseDate(date: string): Date {
  const [y, m, d] = date.split('-').map(Number)
  return new Date(y!, m! - 1, d!)
}

/** A calendar date from the API ("2026-03-03") as "Tue 3 Mar". */
export function formatDay(date: string): string {
  return parseDate(date).toLocaleDateString('en-GB', {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
  })
}

/** "07:45:00" -> "07:45" */
export function shortTime(time: string): string {
  return time.slice(0, 5)
}

/** The wall-clock time of an instant in the given zone, e.g. "07:45". */
export function formatTime(instant: string, timeZone: string): string {
  return new Intl.DateTimeFormat('en-GB', {
    hour: '2-digit',
    minute: '2-digit',
    timeZone,
  }).format(new Date(instant))
}

/** Today's date in a time zone as "YYYY-MM-DD". */
export function todayIn(timeZone: string, now: Date = new Date()): string {
  return new Intl.DateTimeFormat('en-CA', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    timeZone,
  }).format(now)
}

/** Calendar arithmetic on "YYYY-MM-DD" strings. */
export function addDays(date: string, days: number): string {
  const [y, m, d] = date.split('-').map(Number)
  const shifted = new Date(Date.UTC(y!, m! - 1, d! + days))
  return shifted.toISOString().slice(0, 10)
}
