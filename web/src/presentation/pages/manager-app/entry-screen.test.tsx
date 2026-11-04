import { render, screen } from '@testing-library/react'
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

describe('EntryScreen (schedule)', () => {
  it('registers a new plate from the search, a service card and a free slot', async () => {
    const { data, operations } = makeManager()
    render(
      <ManagerContext.Provider value={data}>
        <MemoryRouter initialEntries={['/gerente/agendar']}>
          <Routes>
            <Route
              path="/gerente/agendar"
              element={<EntryScreen mode="schedule" />}
            />
            <Route path="/gerente/agenda" element={<p>agenda</p>} />
          </Routes>
        </MemoryRouter>
      </ManagerContext.Provider>,
    )

    await userEvent.type(screen.getByLabelText('Buscar cliente'), 'abc1d23')
    await userEvent.click(
      await screen.findByRole('button', { name: 'Cadastrar placa ABC1D23' }),
    )
    expect(screen.getByLabelText('Placa *')).toHaveValue('ABC1D23')
    await userEvent.type(screen.getByLabelText('Nome do cliente'), 'Rui')

    await userEvent.click(
      screen.getByRole('radio', { name: /Lavagem Completa/ }),
    )
    await userEvent.click(
      await screen.findByRole('radio', { name: '10h, 2 vagas' }),
    )
    expect(screen.getByRole('radio', { name: '11h, Lotado' })).toBeDisabled()

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

  it('keeps the button disabled until vehicle, service and time are chosen', () => {
    const { data } = makeManager()
    render(
      <ManagerContext.Provider value={data}>
        <MemoryRouter>
          <EntryScreen mode="schedule" />
        </MemoryRouter>
      </ManagerContext.Provider>,
    )

    expect(
      screen.getByRole('button', { name: /Confirmar agendamento/ }),
    ).toBeDisabled()
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
