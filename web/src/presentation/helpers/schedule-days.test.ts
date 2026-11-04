import { describe, expect, it } from 'vitest'
import { dayLabel, upcomingDays } from './schedule-days'

describe('schedule days', () => {
  it('skips Sundays unless the shop opens on Sundays', () => {
    // 2026-11-07 is a Saturday.
    expect(upcomingDays('2026-11-07', false, 3)).toEqual([
      '2026-11-07',
      '2026-11-09',
      '2026-11-10',
    ])
    expect(upcomingDays('2026-11-07', true, 2)).toEqual([
      '2026-11-07',
      '2026-11-08',
    ])
  })

  it('labels today, tomorrow and then the weekday', () => {
    expect(dayLabel('2026-11-07', '2026-11-07')).toBe('Hoje')
    expect(dayLabel('2026-11-08', '2026-11-07')).toBe('Amanhã')
    expect(dayLabel('2026-11-10', '2026-11-07')).toBe('ter')
  })
})
