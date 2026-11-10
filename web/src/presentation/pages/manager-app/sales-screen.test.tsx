import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import type { SaleModel } from '@/domain/models/sale-model'
import { manausDate } from '@/main/config/business-time'
import { ManagerContext, type ManagerData } from './manager-context'
import { SalesScreen } from './sales-screen'

const sale: SaleModel = {
  id: 'sale-1',
  type: 'QUICK_SALE',
  vehicleId: null,
  customerName: 'Ana Souza',
  customerPhone: null,
  subtotal: 70,
  discount: 0,
  total: 70,
  notes: null,
  createdBy: 'u1',
  createdAt: '2026-11-10T15:00:00Z',
  items: [
    {
      id: 'i1',
      servicePriceId: 1,
      description: 'Cera líquida',
      quantity: 2,
      unitPrice: 35,
      total: 70,
    },
  ],
  payments: [{ id: 'p1', method: 'PIX', amount: 70, receivedAt: '' }],
  paidAmount: 70,
  outstandingAmount: 0,
  refunds: [],
  refundedAmount: 0,
}

const renderScreen = (operations: Record<string, unknown>) => {
  const notify = vi.fn()
  render(
    <ManagerContext.Provider
      value={{ operations, notify } as unknown as ManagerData}
    >
      <MemoryRouter>
        <SalesScreen />
      </MemoryRouter>
    </ManagerContext.Provider>,
  )
  return notify
}

describe('SalesScreen', () => {
  it("lists today's quick sales", async () => {
    const listSales = vi.fn().mockResolvedValue([sale])
    renderScreen({ listSales })

    expect(await screen.findByText('Ana Souza')).toBeInTheDocument()
    expect(screen.getByText('2× Cera líquida')).toBeInTheDocument()
    expect(listSales).toHaveBeenCalledWith(
      manausDate(),
      manausDate(),
      'QUICK_SALE',
    )
  })

  it('refunds part of a sale and shows the refunded amount', async () => {
    const refunded: SaleModel = {
      ...sale,
      refundedAmount: 20,
      refunds: [
        {
          id: 'r1',
          saleId: 'sale-1',
          method: 'PIX',
          amount: 20,
          reason: 'CUSTOMER_COMPLAINT',
          description: null,
          createdBy: 'u1',
          createdAt: '2026-11-10T16:00:00Z',
        },
      ],
    }
    const refundSale = vi.fn().mockResolvedValue(refunded)
    const notify = renderScreen({
      listSales: vi.fn().mockResolvedValue([sale]),
      refundSale,
    })

    await userEvent.click(
      await screen.findByRole('button', { name: 'Estornar' }),
    )
    const dialog = screen.getByRole('dialog')
    await userEvent.clear(within(dialog).getByLabelText('Valor a devolver'))
    await userEvent.type(
      within(dialog).getByLabelText('Valor a devolver'),
      '20',
    )
    await userEvent.click(
      within(dialog).getByRole('button', { name: 'Confirmar estorno' }),
    )

    expect(refundSale).toHaveBeenCalledWith('sale-1', {
      method: 'PIX',
      amount: 20,
      reason: 'CUSTOMER_COMPLAINT',
      description: undefined,
    })
    expect(await screen.findByText(/Estornado R\$\s*20,00/)).toBeInTheDocument()
    expect(notify).toHaveBeenCalledWith(
      'success',
      expect.stringContaining('20,00'),
    )
  })
})
