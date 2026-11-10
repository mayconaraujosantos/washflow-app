import { describe, expect, it } from 'vitest'
import { isClosedStatus } from './service-order-model'

describe('isClosedStatus', () => {
  it('treats finalized and cancelled orders as closed', () => {
    expect(isClosedStatus('DONE')).toBe(true)
    expect(isClosedStatus('CANCELLED')).toBe(true)
    expect(isClosedStatus('SCHEDULED')).toBe(false)
    expect(isClosedStatus('READY')).toBe(false)
  })
})
