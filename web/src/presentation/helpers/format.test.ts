import { describe, expect, it } from 'vitest'
import {
  formatBusinessHours,
  formatPhone,
  greeting,
  maskPhoneInput,
  isValidPlate,
  normalizePlate,
  parseMoney,
  phoneDigitsFromInput,
} from './format'

describe('format helpers', () => {
  it.each([
    ['12,50', 12.5],
    ['1.234,56', 1234.56],
    ['12.5', 12.5],
    ['80', 80],
  ])('parses %s as money', (typed, expected) => {
    expect(parseMoney(typed)).toBe(expected)
  })

  it.each(['', 'abc', '1,234', '-5', '12.345'])(
    'rejects %s as money',
    (typed) => {
      expect(parseMoney(typed)).toBeNull()
    },
  )

  it('accepts old and Mercosul plates only', () => {
    expect(isValidPlate('ABC1234')).toBe(true)
    expect(isValidPlate('ABC1D23')).toBe(true)
    expect(isValidPlate('ABC12')).toBe(false)
    expect(isValidPlate('1BC1D23')).toBe(false)
  })

  it('normalizes plates to upper-case alphanumerics', () => {
    expect(normalizePlate('abc-1d23x')).toBe('ABC1D23')
  })

  it('formats Brazilian phones', () => {
    expect(formatPhone('92991234567')).toBe('(92) 99123-4567')
    expect(formatPhone('9233334444')).toBe('(92) 3333-4444')
  })

  it('greets by the Manaus clock', () => {
    // 15:00 UTC is 11:00 in Manaus.
    expect(greeting(new Date('2026-11-03T15:00:00Z'))).toBe('Bom dia')
    expect(greeting(new Date('2026-11-03T23:00:00Z'))).toBe('Boa noite')
  })

  it('masks the phone progressively while typing', () => {
    expect(maskPhoneInput('9')).toBe('(9')
    expect(maskPhoneInput('929912')).toBe('(92) 9912')
    expect(maskPhoneInput('9233334444')).toBe('(92) 3333-4444')
    expect(maskPhoneInput('92991234567')).toBe('(92) 99123-4567')
  })

  it('drops a digit when only a mask character was deleted', () => {
    // "(92) 3333-4444" with the "-" removed by backspace.
    expect(phoneDigitsFromInput('(92) 33334444', '9233334444')).toBe(
      '923333444',
    )
    expect(phoneDigitsFromInput('(92) 3333-44445', '9233334444')).toBe(
      '92333344445',
    )
  })

  it('describes the business hours from the settings', () => {
    expect(
      formatBusinessHours({
        openingHour: 8,
        closingHour: 18,
        openOnSunday: false,
      }),
    ).toBe('seg. a sáb., 08h às 18h')
    expect(
      formatBusinessHours({
        openingHour: 6,
        closingHour: 22,
        openOnSunday: true,
      }),
    ).toBe('todos os dias, 06h às 22h')
  })
})
