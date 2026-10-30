import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { ServiceOrderModel } from '@/domain/models/service-order-model'
import { TrackingSection } from './tracking-section'

const baseOrder: ServiceOrderModel = {
  id: 'order-1',
  vehicleId: 'vehicle-1',
  servicePriceId: 1,
  washerId: 'washer-1',
  status: 'READY',
  scheduledAt: '2026-10-26T16:00:00Z',
  createdAt: '2026-10-26T12:00:00Z',
  updatedAt: '2026-10-26T15:00:00Z',
}

describe('TrackingSection', () => {
  it('keeps the pickup instruction visible while the service is ready', () => {
    render(
      <TrackingSection
        serviceOrders={[baseOrder]}
        vehicles={[]}
        refreshing={false}
        statusNotice={null}
        onRefresh={() => {}}
      />,
    )

    expect(
      screen.getByText(/Seu veículo está pronto para retirada/),
    ).toBeInTheDocument()
  })

  it('explains that a paused wash will resume soon', () => {
    render(
      <TrackingSection
        serviceOrders={[
          { ...baseOrder, status: 'PAUSED', pauseReason: 'LUNCH' },
        ]}
        vehicles={[]}
        refreshing={false}
        statusNotice={null}
        onRefresh={() => {}}
      />,
    )

    expect(screen.getByText('Lavagem pausada')).toBeInTheDocument()
    expect(
      screen.getByText(/Lavagem pausada: almoço. Será retomada em breve/),
    ).toBeInTheDocument()
  })
})
