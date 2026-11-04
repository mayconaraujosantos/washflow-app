import { SubmitEvent, useState } from 'react'
import { Link } from 'react-router-dom'
import type { ScheduleDayModel } from '@/domain/models/establishment-model'
import { ScheduleServiceOrder } from '@/domain/usecases/schedule-service-order'
import { ServiceOrderModel } from '@/domain/models/service-order-model'
import { ServicePriceModel } from '@/domain/models/service-price-model'
import { VehicleModel } from '@/domain/models/vehicle-model'
import { toManausInstant } from '@/main/config/business-time'
import { UiIcon } from '@/presentation/components/ui-icon'
import { EmptyState, Notice } from '@/presentation/components/ui/blocks'
import {
  ServicePicker,
  SlotPicker,
} from '@/presentation/components/ui/schedule-pickers'
import { errorMessage, formatDateTime } from '@/presentation/helpers/format'

// Same minimum lead time DbScheduleServiceOrder enforces for customers.
const CUSTOMER_LEAD_MINUTES = 30

type Props = Readonly<{
  vehicles: VehicleModel[]
  servicePrices: ServicePriceModel[]
  scheduleServiceOrder: ScheduleServiceOrder
  onScheduled: (serviceOrder: ServiceOrderModel) => void
  businessHours?: string
  // Free hours of a day; without it the form falls back to a date/time field.
  loadDay?: (date: string) => Promise<ScheduleDayModel>
  openOnSunday?: boolean
}>

export function ScheduleSection({
  vehicles,
  servicePrices,
  scheduleServiceOrder,
  onScheduled,
  businessHours = 'seg. a sáb., 08h às 18h',
  loadDay,
  openOnSunday = false,
}: Props) {
  const [vehicleId, setVehicleId] = useState(
    vehicles.length === 1 ? vehicles[0].id : '',
  )
  const [servicePriceId, setServicePriceId] = useState<number | null>(null)
  const [slot, setSlot] = useState<string | null>(null)
  const [scheduledAt, setScheduledAt] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const chosenTime = loadDay ? slot : scheduledAt
  const ready = Boolean(vehicleId && servicePriceId && chosenTime)

  const handleSubmit = async (event: SubmitEvent) => {
    event.preventDefault()
    setError(null)
    setSuccess(null)
    if (!ready || servicePriceId === null) {
      setError('Escolha o veículo, o serviço e o horário.')
      return
    }
    setLoading(true)
    try {
      const startsAt = loadDay && slot ? slot : toManausInstant(scheduledAt)
      const serviceOrder = await scheduleServiceOrder.schedule({
        vehicleId,
        servicePriceId,
        scheduledAt: startsAt,
      })
      onScheduled(serviceOrder)
      setSuccess(`Lavagem agendada para ${formatDateTime(startsAt)}!`)
      setSlot(null)
      setScheduledAt('')
    } catch (cause) {
      setError(errorMessage(cause, 'Erro ao agendar'))
    } finally {
      setLoading(false)
    }
  }

  if (vehicles.length === 0) {
    return (
      <EmptyState
        icon="car"
        title="Cadastre um veículo antes de agendar."
        description={
          <Link to="/cliente/veiculos" className="btn btn-accent">
            Cadastrar veículo
          </Link>
        }
      />
    )
  }

  return (
    <form className="form" onSubmit={handleSubmit}>
      <section className="step">
        <h2 className="step-title">
          <span className="step-number">1</span> Veículo
        </h2>
        <div className="choice-grid" role="radiogroup" aria-label="Veículo">
          {vehicles.map((vehicle) => (
            <button
              key={vehicle.id}
              type="button"
              role="radio"
              aria-checked={vehicleId === vehicle.id}
              className="choice"
              onClick={() => setVehicleId(vehicle.id)}
            >
              <span className="plate">{vehicle.plate}</span>
              <span className="faint">
                {[vehicle.model, vehicle.color].filter(Boolean).join(' · ') ||
                  'Veículo'}
              </span>
            </button>
          ))}
        </div>
      </section>

      <section className="step">
        <h2 className="step-title">
          <span className="step-number">2</span> Serviço
        </h2>
        <ServicePicker
          services={servicePrices}
          value={servicePriceId}
          onChange={setServicePriceId}
        />
      </section>

      <section className="step">
        <h2 className="step-title">
          <span className="step-number">3</span> Dia e horário
        </h2>
        {loadDay ? (
          <SlotPicker
            loadDay={loadDay}
            openOnSunday={openOnSunday}
            value={slot}
            onChange={setSlot}
            leadMinutes={CUSTOMER_LEAD_MINUTES}
          />
        ) : (
          <label className="field">
            <span>Data e horário (Manaus · {businessHours})</span>
            <input
              type="datetime-local"
              value={scheduledAt}
              onChange={(event) => setScheduledAt(event.target.value)}
              required
            />
          </label>
        )}
      </section>

      <Notice tone="error">{error}</Notice>
      <Notice tone="success">{success}</Notice>

      <button
        type="submit"
        className="btn btn-accent btn-block"
        disabled={loading || !ready}
      >
        <UiIcon name="calendar" size={18} />
        {loading ? 'Agendando...' : 'Agendar'}
      </button>
    </form>
  )
}
