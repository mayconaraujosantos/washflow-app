import { describe, expect, it } from 'vitest'
import { refundableByMethod, type SaleModel } from './sale-model'

const sale = {
  payments: [
    { id: 'p1', method: 'PIX', amount: 50, receivedAt: '' },
    { id: 'p2', method: 'CASH', amount: 20, receivedAt: '' },
    { id: 'p3', method: 'PIX', amount: 10, receivedAt: '' },
  ],
  refunds: [
    {
      id: 'r1',
      saleId: 's1',
      method: 'PIX',
      amount: 25.5,
      reason: 'WRONG_CHARGE',
      description: null,
      createdBy: 'u1',
      createdAt: '',
    },
  ],
} as unknown as SaleModel

describe('refundableByMethod', () => {
  it('is what came in through each method minus what was refunded', () => {
    expect(refundableByMethod(sale)).toEqual({ PIX: 34.5, CASH: 20 })
  })

  it('drops methods with nothing left', () => {
    expect(
      refundableByMethod({
        ...sale,
        refunds: [{ ...sale.refunds[0], method: 'CASH', amount: 20 }],
      }),
    ).toEqual({ PIX: 60 })
  })
})
