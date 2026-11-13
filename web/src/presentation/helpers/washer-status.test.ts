import { describe, expect, it } from 'vitest'
import type { WasherStatusModel } from '@/domain/models/washer-status-model'
import {
  minutesSince,
  olderThan,
  reservedText,
  washerStateText,
} from './washer-status'

const now = new Date('2026-11-20T15:00:00Z')
const washer = (
  overrides: Partial<WasherStatusModel> = {},
): WasherStatusModel => ({
  id: 'w1',
  name: 'Lucas',
  phone: null,
  state: 'FREE',
  current: null,
  reserved: 0,
  ...overrides,
})

describe('washer status texts', () => {
  it('counts minutes and hours', () => {
    expect(minutesSince('2026-11-20T14:59:40Z', now)).toBe('agora')
    expect(minutesSince('2026-11-20T14:48:00Z', now)).toBe('há 12 min')
    expect(minutesSince('2026-11-20T13:00:00Z', now)).toBe('há 2 h')
    expect(minutesSince('2026-11-20T13:35:00Z', now)).toBe('há 1 h 25 min')
    expect(olderThan('2026-11-20T14:30:00Z', 20 * 60_000, now)).toBe(true)
    expect(olderThan('2026-11-20T14:50:00Z', 20 * 60_000, now)).toBe(false)
  })

  it('describes free, washing and paused washers', () => {
    const current = {
      serviceOrderId: 'o1',
      vehiclePlate: 'ABC1D23',
      vehicleModel: 'Onix',
      since: '2026-11-20T14:48:00Z',
      pauseReason: null,
      pausedAt: null,
    }

    expect(washerStateText(washer(), now)).toBe('Livre')
    expect(washerStateText(washer({ state: 'WASHING', current }), now)).toBe(
      'Lavando ABC1D23 há 12 min',
    )
    expect(
      washerStateText(
        washer({
          state: 'PAUSED',
          current: {
            ...current,
            pauseReason: 'LUNCH',
            pausedAt: '2026-11-20T14:55:00Z',
          },
        }),
        now,
      ),
    ).toBe('Em pausa (almoço) há 5 min · ABC1D23')
  })

  it('says how many cars are reserved', () => {
    expect(reservedText(washer())).toBe('')
    expect(reservedText(washer({ reserved: 1 }))).toBe('1 carro reservado')
    expect(reservedText(washer({ reserved: 3 }))).toBe('3 carros reservados')
  })
})
