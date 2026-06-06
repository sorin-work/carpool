import { afterAll, beforeAll, describe, expect, it } from 'vitest'

import { formatDay, parseDate } from '@/utils/dates'

// Calendar dates must not move when the browser is behind UTC.
describe('calendar dates west of UTC', () => {
  let original: string | undefined

  beforeAll(() => {
    original = process.env.TZ
    process.env.TZ = 'America/Los_Angeles'
  })

  afterAll(() => {
    process.env.TZ = original
  })

  it('keeps the weekday and day of month', () => {
    expect(formatDay('2026-03-03')).toBe('Tue 3 Mar')
    expect(formatDay('2026-11-01')).toBe('Sun 1 Nov')
  })

  it('parses to local midnight', () => {
    const date = parseDate('2026-03-03')
    expect([date.getFullYear(), date.getMonth(), date.getDate(), date.getHours()]).toEqual([
      2026, 2, 3, 0,
    ])
  })
})
