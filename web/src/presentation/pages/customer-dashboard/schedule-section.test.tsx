import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import type { ScheduleServiceOrder } from '@/domain/usecases/schedule-service-order'
import type { ServicePriceModel } from '@/domain/models/service-price-model'
import type { VehicleModel } from '@/domain/models/vehicle-model'
import { ScheduleSection } from './schedule-section'

const services: ServicePriceModel[] = [
  { id: 1, name: 'Lavagem completa', price: 60, washerCommission: 20 },
  { id: 2, name: 'Polimento', price: 45, washerCommission: 5 },
]

const vehicle: VehicleModel = {
  id: 'vehicle-1',
  customerId: 'customer-1',
  plate: 'ABC1D23',
  model: 'Onix',
  color: 'Prata',
}

describe('ScheduleSection', () => {
  it('lets the customer book one or multiple services as one appointment', async () => {
    const schedule = vi.fn().mockResolvedValue({ id: 'order-1' })
    const scheduleServiceOrder: ScheduleServiceOrder = { schedule }
    const onScheduled = vi.fn()
    render(
      <MemoryRouter>
        <ScheduleSection
          vehicles={[vehicle]}
          servicePrices={services}
          scheduleServiceOrder={scheduleServiceOrder}
          onScheduled={onScheduled}
        />
      </MemoryRouter>,
    )

    await userEvent.click(
      screen.getByRole('checkbox', { name: /Lavagem completa/ }),
    )
    expect(screen.getByText('Total dos serviços:')).toHaveTextContent('60,00')

    await userEvent.click(screen.getByRole('checkbox', { name: /Polimento/ }))
    expect(screen.getByText('Total dos serviços:')).toHaveTextContent('105,00')

    await userEvent.type(
      screen.getByLabelText(/Data e horário/),
      '2030-01-02T10:00',
    )
    await userEvent.click(screen.getByRole('button', { name: 'Agendar' }))

    expect(schedule).toHaveBeenCalledWith({
      vehicleId: vehicle.id,
      servicePriceIds: [1, 2],
      scheduledAt: expect.any(String),
    })
    expect(onScheduled).toHaveBeenCalledWith({ id: 'order-1' })
  })
})
