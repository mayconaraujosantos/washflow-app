import { describe, expect, it } from 'vitest'
import { BUSINESS_TIME_ZONE, toManausInstant } from './business-time'

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
