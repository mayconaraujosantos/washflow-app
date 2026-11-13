import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import type { ServiceOrderModel } from '@/domain/models/service-order-model'
import type { WasherStatusModel } from '@/domain/models/washer-status-model'
import { ManagerContext, type ManagerData } from './manager-context'
import { WasherBoard } from './washer-board'
import { WasherPickerSheet } from './washer-picker-sheet'

const lucas: WasherStatusModel = {
  id: 'w1',
  name: 'Lucas',
  phone: null,
  state: 'FREE',
  current: null,
  reserved: 1,
}
const bruno: WasherStatusModel = {
  id: 'w2',
  name: 'Bruno',
  phone: null,
  state: 'WASHING',
  current: {
    serviceOrderId: 'o9',
    vehiclePlate: 'XYZ9Z99',
    vehicleModel: null,
    since: new Date().toISOString(),
    pauseReason: null,
    pausedAt: null,
  },
  reserved: 0,
}

const withManager = (
  ui: React.ReactNode,
  loadWasherBoard: () => Promise<WasherStatusModel[]>,
) =>
  render(
    <ManagerContext.Provider
      value={
        { operations: { loadWasherBoard }, yard: [] } as unknown as ManagerData
      }
    >
      <MemoryRouter>{ui}</MemoryRouter>
    </ManagerContext.Provider>,
  )

describe('WasherBoard', () => {
  it('shows who is free and who is washing', async () => {
    withManager(<WasherBoard />, () => Promise.resolve([lucas, bruno]))

    expect(await screen.findByText('(1 livre de 2)')).toBeInTheDocument()
    expect(screen.getByText('Livre · 1 carro reservado')).toBeInTheDocument()
    expect(screen.getByText('Lavando XYZ9Z99 agora')).toBeInTheDocument()
  })

  it('warns when the lava-jato has no active washer', async () => {
    withManager(<WasherBoard />, () => Promise.resolve([]))

    expect(await screen.findByText(/Nenhum lavador ativo/)).toBeInTheDocument()
    expect(
      screen.getByRole('link', { name: 'Cadastrar lavador' }),
    ).toHaveAttribute('href', '/gerente/equipe')
  })
})

describe('WasherPickerSheet', () => {
  const order = {
    id: 'o1',
    washerId: null,
    details: { vehiclePlate: 'ABC1D23', serviceName: 'Completa' },
  } as unknown as ServiceOrderModel

  it('reserves the car for the chosen washer', async () => {
    const onConfirm = vi.fn()
    withManager(
      <WasherPickerSheet
        order={order}
        title="Receber no pátio"
        confirmLabel="Receber no pátio"
        onClose={vi.fn()}
        onConfirm={onConfirm}
      />,
      () => Promise.resolve([lucas, bruno]),
    )

    fireEvent.click(await screen.findByRole('button', { name: /Lucas/ }))
    fireEvent.click(screen.getByRole('button', { name: 'Receber no pátio' }))

    expect(onConfirm).toHaveBeenCalledWith(order, 'w1')
  })

  it('sends the car to the shared queue by default', async () => {
    const onConfirm = vi.fn()
    withManager(
      <WasherPickerSheet
        order={order}
        title="Receber no pátio"
        confirmLabel="Receber no pátio"
        onClose={vi.fn()}
        onConfirm={onConfirm}
      />,
      () => Promise.resolve([lucas]),
    )

    await screen.findByRole('button', { name: /Lucas/ })
    fireEvent.click(screen.getByRole('button', { name: 'Receber no pátio' }))

    expect(onConfirm).toHaveBeenCalledWith(order, null)
  })
})
