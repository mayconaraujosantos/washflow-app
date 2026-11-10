import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { ServiceOrderModel } from '@/domain/models/service-order-model'
import { CancelSheet } from './cancel-sheet'

const order = {
  id: 'order-1',
  status: 'SCHEDULED',
  details: { vehiclePlate: 'ABC1D23' },
} as ServiceOrderModel

describe('CancelSheet', () => {
  it('confirms with the chosen reason', async () => {
    const onConfirm = vi.fn()
    render(
      <CancelSheet order={order} onClose={vi.fn()} onConfirm={onConfirm} />,
    )

    expect(screen.getByText('ABC1D23')).toBeInTheDocument()
    await userEvent.click(
      screen.getByRole('button', { name: 'Não compareceu' }),
    )
    await userEvent.click(
      screen.getByRole('button', { name: 'Cancelar atendimento' }),
    )

    expect(onConfirm).toHaveBeenCalledWith(order, 'NO_SHOW', undefined)
  })

  it('requires a description for "Outro"', async () => {
    const onConfirm = vi.fn()
    render(
      <CancelSheet order={order} onClose={vi.fn()} onConfirm={onConfirm} />,
    )

    await userEvent.click(screen.getByRole('button', { name: 'Outro' }))
    const confirm = screen.getByRole('button', { name: 'Cancelar atendimento' })
    expect(confirm).toBeDisabled()

    await userEvent.type(screen.getByLabelText('Descreva o motivo'), 'Chuva')
    await userEvent.click(confirm)

    expect(onConfirm).toHaveBeenCalledWith(order, 'OTHER', 'Chuva')
  })

  it('renders nothing without an order', () => {
    const { container } = render(
      <CancelSheet order={null} onClose={vi.fn()} onConfirm={vi.fn()} />,
    )
    expect(container).toBeEmptyDOMElement()
  })
})
