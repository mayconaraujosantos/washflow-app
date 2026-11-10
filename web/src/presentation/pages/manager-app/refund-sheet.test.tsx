import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { ServiceOrderModel } from '@/domain/models/service-order-model'
import { RefundSheet } from './refund-sheet'

const order = {
  id: 'order-1',
  status: 'DONE',
  details: { vehiclePlate: 'ABC1D23', serviceName: 'Lavagem Completa' },
} as ServiceOrderModel

const renderSheet = (onConfirm = vi.fn()) => {
  render(
    <RefundSheet
      order={order}
      amount={60}
      onClose={vi.fn()}
      onConfirm={onConfirm}
    />,
  )
  return onConfirm
}

describe('RefundSheet', () => {
  it('refunds in the original method keeping the commission by default', async () => {
    const onConfirm = renderSheet()

    expect(screen.getByText('ABC1D23')).toBeInTheDocument()
    expect(screen.getByText(/R\$\s*60,00/)).toBeInTheDocument()
    expect(
      screen.getByRole('checkbox', { name: /Estornar a comissão do lavador/ }),
    ).not.toBeChecked()
    await userEvent.click(
      screen.getByRole('button', { name: 'Cobrança errada' }),
    )
    await userEvent.click(
      screen.getByRole('button', { name: 'Confirmar estorno' }),
    )

    expect(onConfirm).toHaveBeenCalledWith(order, {
      reason: 'WRONG_CHARGE',
      description: undefined,
      refundMethod: undefined,
      reverseCommission: false,
    })
  })

  it('can refund in another method and reverse the commission', async () => {
    const onConfirm = renderSheet()

    await userEvent.selectOptions(screen.getByLabelText('Devolver em'), 'CASH')
    await userEvent.click(
      screen.getByRole('checkbox', { name: /Estornar a comissão do lavador/ }),
    )
    await userEvent.click(
      screen.getByRole('button', { name: 'Confirmar estorno' }),
    )

    expect(onConfirm).toHaveBeenCalledWith(order, {
      reason: 'CUSTOMER_COMPLAINT',
      description: undefined,
      refundMethod: 'CASH',
      reverseCommission: true,
    })
  })

  it('requires a description for "Outro"', async () => {
    const onConfirm = renderSheet()

    await userEvent.click(screen.getByRole('button', { name: 'Outro' }))
    const confirm = screen.getByRole('button', { name: 'Confirmar estorno' })
    expect(confirm).toBeDisabled()
    await userEvent.type(screen.getByLabelText('Descreva o motivo'), 'Riscou')
    await userEvent.click(confirm)

    expect(onConfirm).toHaveBeenCalledWith(
      order,
      expect.objectContaining({ reason: 'OTHER', description: 'Riscou' }),
    )
  })
})
