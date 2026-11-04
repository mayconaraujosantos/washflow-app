import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import type { ScheduleDayModel } from '@/domain/models/establishment-model'
import { manausDate } from '@/main/config/business-time'
import { SlotPicker } from '@/presentation/components/ui/schedule-pickers'
import { EntryScreen } from './entry-screen'
import { ManagerContext, type ManagerData } from './manager-context'

const FUTURE = '2099-01-05T14:00:00Z'
const FULL = '2099-01-05T15:00:00Z'

const day = (date: string): ScheduleDayModel => ({
  date,
  open: true,
  slots: [
    { startsAt: FUTURE, hour: 10, booked: 1, capacity: 3, available: true },
    { startsAt: FULL, hour: 11, booked: 3, capacity: 3, available: false },
  ],
})

const makeManager = () => {
  const operations = {
    searchVehicles: vi.fn().mockResolvedValue([]),
    registerEntry: vi.fn().mockResolvedValue({ id: 'order-1' }),
  }
  const establishment = {
    availability: vi.fn((date: string) => Promise.resolve(day(date))),
  }
  const data = {
    operations,
    establishment,
    servicePrices: [
      { id: 1, name: 'Lavagem Completa', price: 60, washerCommission: 20 },
      { id: 2, name: 'Polimento', price: 120, washerCommission: 35 },
    ],
    settings: {
      name: 'Lava',
      openingHour: 8,
      closingHour: 18,
      openOnSunday: false,
      maxVehiclesPerHour: 3,
      configuredAt: '2026-11-01T00:00:00Z',
    },
    notify: vi.fn(),
    refresh: vi.fn().mockResolvedValue(undefined),
  } as unknown as ManagerData
  return { data, operations }
}

const renderEntry = (mode: 'entry' | 'schedule', data: ManagerData) =>
  render(
    <ManagerContext.Provider value={data}>
      <MemoryRouter initialEntries={['/gerente/form']}>
        <Routes>
          <Route path="/gerente/form" element={<EntryScreen mode={mode} />} />
          <Route path="/gerente/agenda" element={<p>agenda</p>} />
          <Route path="/gerente/patio" element={<p>patio</p>} />
        </Routes>
      </MemoryRouter>
    </ManagerContext.Provider>,
  )

describe('EntryScreen (reference layout)', () => {
  it('shows "Nenhum cliente selecionado" with the add-client button first', () => {
    renderEntry('entry', makeManager().data)

    expect(screen.getByText('Nenhum cliente selecionado')).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: 'Adicionar novo cliente' }),
    ).toBeInTheDocument()
    expect(screen.getByText('Nenhum produto selecionado')).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: 'Adicionar produto ou serviço' }),
    ).toBeInTheDocument()
  })

  it('schedules a new client from the search, a catalog service and a free slot', async () => {
    const { data, operations } = makeManager()
    renderEntry('schedule', data)

    await userEvent.type(screen.getByLabelText('Buscar cliente'), 'abc1d23')
    await userEvent.click(
      await screen.findByRole('button', { name: 'Cadastrar placa ABC1D23' }),
    )
    const sheet = screen.getByRole('dialog', { name: 'Novo cliente' })
    expect(within(sheet).getByLabelText('Placa')).toHaveValue('ABC1D23')
    await userEvent.type(within(sheet).getByLabelText('Nome'), 'Rui')
    await userEvent.click(
      within(sheet).getByRole('button', { name: 'Salvar cliente' }),
    )

    await userEvent.click(
      await screen.findByRole('radio', { name: '10h, 2 vagas' }),
    )
    expect(screen.getByRole('radio', { name: '11h, Lotado' })).toBeDisabled()

    await userEvent.click(
      screen.getByRole('button', { name: 'Adicionar produto ou serviço' }),
    )
    await userEvent.click(
      within(
        screen.getByRole('dialog', { name: 'Selecione o produto ou serviço' }),
      ).getByRole('button', { name: /Lavagem Completa/ }),
    )
    expect(screen.getByText(/^Total R\$\s60,00$/)).toBeInTheDocument()

    await userEvent.click(
      screen.getByRole('button', { name: /Confirmar agendamento/ }),
    )

    expect(operations.registerEntry).toHaveBeenCalledWith(
      expect.objectContaining({
        plate: 'ABC1D23',
        servicePriceId: 1,
        customerName: 'Rui',
        scheduledAt: FUTURE,
      }),
    )
    expect(await screen.findByText('agenda')).toBeInTheDocument()
  })

  it('explains what is missing instead of submitting', async () => {
    const { data, operations } = makeManager()
    renderEntry('entry', data)

    await userEvent.click(
      screen.getByRole('button', { name: /Registrar entrada/ }),
    )

    expect(
      screen.getByText('Busque o cliente ou adicione um novo cliente.'),
    ).toBeInTheDocument()
    expect(operations.registerEntry).not.toHaveBeenCalled()
  })
})

describe('SlotPicker', () => {
  it('blocks hours inside the customer lead time', async () => {
    const soon = new Date(Date.now() + 10 * 60_000).toISOString()
    const later = new Date(Date.now() + 3 * 60 * 60_000).toISOString()
    const loadDay = vi.fn(async (date: string) => ({
      date,
      open: true,
      slots: [
        { startsAt: soon, hour: 9, booked: 0, capacity: 2, available: true },
        { startsAt: later, hour: 12, booked: 0, capacity: 2, available: true },
      ],
    }))
    render(
      <SlotPicker
        loadDay={loadDay}
        openOnSunday={false}
        value={null}
        onChange={vi.fn()}
        leadMinutes={30}
      />,
    )

    expect(
      await screen.findByRole('radio', { name: '9h, Encerrado' }),
    ).toBeDisabled()
    expect(screen.getByRole('radio', { name: '12h, 2 vagas' })).toBeEnabled()
    expect(loadDay).toHaveBeenCalledWith(manausDate())
  })

  it('moves on to the next day with a free hour when today is over', async () => {
    const past = new Date(Date.now() - 60 * 60_000).toISOString()
    const future = new Date(Date.now() + 26 * 60 * 60_000).toISOString()
    const loadDay = vi.fn(async (date: string) => ({
      date,
      open: true,
      slots:
        date === manausDate()
          ? [
              {
                startsAt: past,
                hour: 8,
                booked: 0,
                capacity: 2,
                available: false,
              },
            ]
          : [
              {
                startsAt: future,
                hour: 9,
                booked: 0,
                capacity: 2,
                available: true,
              },
            ],
    }))
    render(
      <SlotPicker
        loadDay={loadDay}
        openOnSunday
        value={null}
        onChange={vi.fn()}
      />,
    )

    expect(
      await screen.findByRole('radio', { name: '9h, 2 vagas' }),
    ).toBeEnabled()
    expect(screen.getByRole('radio', { name: /Amanhã/ })).toHaveAttribute(
      'aria-checked',
      'true',
    )
  })
})
