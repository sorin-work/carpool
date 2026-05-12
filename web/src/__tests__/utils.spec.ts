import { describe, expect, it } from 'vitest'

import { addDays, describeDays, formatTime, shortTime, todayIn } from '@/utils/dates'
import { avatarHue, initials } from '@/utils/initials'
import { escapeHtml, renderMessage } from '@/utils/message'
import { formatCents, formatKm } from '@/utils/money'

describe('dates', () => {
  it('describes common day patterns', () => {
    expect(describeDays(['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'])).toBe('Mon–Fri')
    expect(describeDays(['FRIDAY', 'MONDAY', 'WEDNESDAY'])).toBe('Mon, Wed, Fri')
    expect(
      describeDays(['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']),
    ).toBe('Every day')
  })

  it('adds days across month and year ends', () => {
    expect(addDays('2026-02-27', 2)).toBe('2026-03-01')
    expect(addDays('2026-12-31', 1)).toBe('2027-01-01')
    expect(addDays('2026-03-02', -7)).toBe('2026-02-23')
  })

  it('shows departure times in the group zone', () => {
    expect(formatTime('2026-03-02T05:45:00Z', 'Europe/Bucharest')).toBe('07:45')
    expect(formatTime('2026-03-02T05:45:00Z', 'Europe/London')).toBe('05:45')
  })

  it('knows the date in another zone', () => {
    const lateEvening = new Date('2026-03-02T23:30:00Z')
    expect(todayIn('Europe/Bucharest', lateEvening)).toBe('2026-03-03')
    expect(todayIn('America/New_York', lateEvening)).toBe('2026-03-02')
  })

  it('trims seconds', () => {
    expect(shortTime('07:45:00')).toBe('07:45')
  })
})

describe('initials', () => {
  it('takes the first and last name', () => {
    expect(initials('Ana Maria Popescu')).toBe('AP')
    expect(initials('ben')).toBe('B')
    expect(initials('  ')).toBe('?')
  })

  it('gives the same colour to the same name', () => {
    expect(avatarHue('Ana')).toBe(avatarHue('Ana'))
    expect(avatarHue('Ana')).toBeLessThan(360)
  })
})

describe('money', () => {
  it('formats cents in the group currency', () => {
    expect(formatCents(1234, 'EUR')).toBe('€12.34')
    expect(formatCents(-80, 'GBP')).toBe('-£0.80')
  })

  it('rounds distances', () => {
    expect(formatKm(6.44)).toBe('6.4 km')
    expect(formatKm(12)).toBe('12 km')
  })
})

describe('notification messages', () => {
  it('bolds names', () => {
    expect(renderMessage('**Ana** will drive **School run**.')).toBe(
      '<strong>Ana</strong> will drive <strong>School run</strong>.',
    )
  })
})

describe('notification markup', () => {
  it('escapes html coming from trip names and reasons', () => {
    const message = '**<img src=x onerror=alert(1)>** was cancelled: <script>steal()</script>'

    expect(renderMessage(message)).toBe(
      '<strong>&lt;img src=x onerror=alert(1)&gt;</strong> was cancelled: &lt;script&gt;steal()&lt;/script&gt;',
    )
  })

  it('escapes quotes and ampersands', () => {
    expect(escapeHtml(`Tom & "Jerry's" run`)).toBe('Tom &amp; &quot;Jerry&#39;s&quot; run')
  })
})
