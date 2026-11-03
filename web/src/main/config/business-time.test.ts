import { describe, expect, it } from 'vitest'
import {
  BUSINESS_TIME_ZONE,
  addDays,
  manausDate,
  manausHour,
  toManausInstant,
  toManausLocalInput,
} from './business-time'

describe('business time zone', () => {
  it('uses Manaus as the shop timezone', () => {
    expect(BUSINESS_TIME_ZONE).toBe('America/Manaus')
  })

  it('converts the selected Manaus opening time to the corresponding UTC instant', () => {
    expect(toManausInstant('2026-10-26T08:00')).toBe('2026-10-26T12:00:00.000Z')
  })

  it('converts the selected Manaus closing time to the corresponding UTC instant', () => {
    expect(toManausInstant('2026-10-26T18:00')).toBe('2026-10-26T22:00:00.000Z')
  })

  it('rejects malformed and impossible local date-times', () => {
    expect(() => toManausInstant('not-a-date')).toThrow(RangeError)
    expect(() => toManausInstant('2026-02-30T08:00')).toThrow(RangeError)
  })
})

describe('Manaus civil date helpers', () => {
  it('uses the Manaus date, not the UTC date, near midnight', () => {
    // 02:30 UTC is still 22:30 of the previous day in Manaus (UTC-4).
    expect(manausDate('2026-11-04T02:30:00Z')).toBe('2026-11-03')
    expect(manausHour('2026-11-04T02:30:00Z')).toBe(22)
  })

  it('formats an instant as a Manaus datetime-local value', () => {
    expect(toManausLocalInput('2026-11-03T14:05:00Z')).toBe('2026-11-03T10:05')
  })

  it('adds days across month boundaries', () => {
    expect(addDays('2026-11-30', 1)).toBe('2026-12-01')
    expect(addDays('2026-03-01', -1)).toBe('2026-02-28')
  })
})
