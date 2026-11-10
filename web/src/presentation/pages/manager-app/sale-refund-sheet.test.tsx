import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { SaleModel } from '@/domain/models/sale-model'
import { SaleRefundSheet } from './sale-refund-sheet'

const sale = {
  id: 'sale-1',
  customerName: 'Ana',
  total: 70,
  payments: [
    { id: 'p1', method: 'PIX', amount: 50, receivedAt: '' },
    { id: 'p2', method: 'CASH', amount: 20, receivedAt: '' },
  ],
  refunds: [],
  refundedAmount: 0,
} as unknown as SaleModel

const renderSheet = () => {
  const onConfirm = vi.fn()
  render(
    <SaleRefundSheet sale={sale} onClose={vi.fn()} onConfirm={onConfirm} />,
  )
  return onConfirm
}

describe('SaleRefundSheet', () => {
  it('offers the full balance of the first method by default', async () => {
    const onConfirm = renderSheet()

    expect(screen.getByLabelText('Valor a devolver')).toHaveValue('50,00')
    await userEvent.click(
      screen.getByRole('button', { name: 'Confirmar estorno' }),
    )

    expect(onConfirm).toHaveBeenCalledWith(sale, {
      method: 'PIX',
      amount: 50,
      reason: 'CUSTOMER_COMPLAINT',
      description: undefined,
    })
  })

  it('refunds part of another method', async () => {
    const onConfirm = renderSheet()

    await userEvent.selectOptions(screen.getByLabelText('Devolver em'), 'CASH')
    expect(screen.getByLabelText('Valor a devolver')).toHaveValue('20,00')
    await userEvent.clear(screen.getByLabelText('Valor a devolver'))
    await userEvent.type(screen.getByLabelText('Valor a devolver'), '7,5')
    await userEvent.click(
      screen.getByRole('button', { name: 'Cobrança errada' }),
    )
    await userEvent.click(
      screen.getByRole('button', { name: 'Confirmar estorno' }),
    )

    expect(onConfirm).toHaveBeenCalledWith(sale, {
      method: 'CASH',
      amount: 7.5,
      reason: 'WRONG_CHARGE',
      description: undefined,
    })
  })

  it('blocks amounts above what the method still has', async () => {
    const onConfirm = renderSheet()

    await userEvent.clear(screen.getByLabelText('Valor a devolver'))
    await userEvent.type(screen.getByLabelText('Valor a devolver'), '50.01')

    expect(screen.getByText('Máximo em Pix: R$ 50,00')).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: 'Confirmar estorno' }),
    ).toBeDisabled()
    expect(onConfirm).not.toHaveBeenCalled()
  })
})
