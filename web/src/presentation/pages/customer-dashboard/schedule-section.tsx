import { SubmitEvent, useState } from 'react'
import { Link } from 'react-router-dom'
import { ScheduleServiceOrder } from '@/domain/usecases/schedule-service-order'
import { ServiceOrderModel } from '@/domain/models/service-order-model'
import { ServicePriceModel } from '@/domain/models/service-price-model'
import { VehicleModel } from '@/domain/models/vehicle-model'
import { toManausInstant } from '@/main/config/business-time'
import { UiIcon } from '@/presentation/components/ui-icon'
import { EmptyState, Notice } from '@/presentation/components/ui/blocks'
import { errorMessage, formatMoney } from '@/presentation/helpers/format'

type Props = Readonly<{
  vehicles: VehicleModel[]
  servicePrices: ServicePriceModel[]
  scheduleServiceOrder: ScheduleServiceOrder
  onScheduled: (serviceOrder: ServiceOrderModel) => void
  businessHours?: string
}>

export function ScheduleSection({
  vehicles,
  servicePrices,
  scheduleServiceOrder,
  onScheduled,
  businessHours = 'seg. a sáb., 08h às 18h',
}: Props) {
  const [vehicleId, setVehicleId] = useState('')
  const [servicePriceId, setServicePriceId] = useState('')
  const [scheduledAt, setScheduledAt] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (event: SubmitEvent) => {
    event.preventDefault()
    setError(null)
    setSuccess(null)
    setLoading(true)
    try {
      const serviceOrder = await scheduleServiceOrder.schedule({
        vehicleId,
        servicePriceId: Number(servicePriceId),
        scheduledAt: toManausInstant(scheduledAt),
      })
      onScheduled(serviceOrder)
      setSuccess('Lavagem agendada com sucesso!')
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
      <label className="field">
        <span>Veículo</span>
        <select
          value={vehicleId}
          onChange={(event) => setVehicleId(event.target.value)}
          required
        >
          <option value="" disabled>
            Selecione...
          </option>
          {vehicles.map((vehicle) => (
            <option key={vehicle.id} value={vehicle.id}>
              {vehicle.plate}
              {vehicle.model ? ` · ${vehicle.model}` : ''}
            </option>
          ))}
        </select>
      </label>

      <label className="field">
        <span>Serviço</span>
        <select
          value={servicePriceId}
          onChange={(event) => setServicePriceId(event.target.value)}
          required
        >
          <option value="" disabled>
            Selecione...
          </option>
          {servicePrices.map((servicePrice) => (
            <option key={servicePrice.id} value={servicePrice.id}>
              {servicePrice.name} · {formatMoney(servicePrice.price)}
            </option>
          ))}
        </select>
      </label>

      <label className="field">
        <span>Data e horário (Manaus · {businessHours})</span>
        <input
          type="datetime-local"
          value={scheduledAt}
          onChange={(event) => setScheduledAt(event.target.value)}
          required
        />
      </label>

      <Notice tone="error">{error}</Notice>
      <Notice tone="success">{success}</Notice>

      <button
        type="submit"
        className="btn btn-accent btn-block"
        disabled={loading}
      >
        <UiIcon name="calendar" size={18} />
        {loading ? 'Agendando...' : 'Agendar'}
      </button>
    </form>
  )
}
