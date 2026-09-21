import { SubmitEvent, useState } from 'react'
import { ScheduleServiceOrder } from '@/domain/usecases/schedule-service-order'
import { ServiceOrderModel } from '@/domain/models/service-order-model'
import { ServicePriceModel } from '@/domain/models/service-price-model'
import { VehicleModel } from '@/domain/models/vehicle-model'

type Props = Readonly<{
  vehicles: VehicleModel[]
  servicePrices: ServicePriceModel[]
  scheduleServiceOrder: ScheduleServiceOrder
  onScheduled: (serviceOrder: ServiceOrderModel) => void
}>

export function ScheduleSection({
  vehicles,
  servicePrices,
  scheduleServiceOrder,
  onScheduled,
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
      // datetime-local has no timezone info - `new Date(...)` reads it as the
      // browser's local time, and toISOString() converts that to the correct
      // absolute instant. DbScheduleServiceOrder then judges business hours
      // against that instant in the shop's own timezone (America/Sao_Paulo),
      // not the customer's - the physically correct behavior either way.
      const serviceOrder = await scheduleServiceOrder.schedule({
        vehicleId,
        servicePriceId: Number(servicePriceId),
        scheduledAt: new Date(scheduledAt).toISOString(),
      })
      onScheduled(serviceOrder)
      setSuccess('Lavagem agendada com sucesso!')
      setScheduledAt('')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao agendar')
    } finally {
      setLoading(false)
    }
  }

  return (
    <section className="dashboard-section">
      <h2>Agendar lavagem</h2>

      {vehicles.length === 0 ? (
        <p className="empty-state">Cadastre um veículo antes de agendar.</p>
      ) : (
        <form className="stacked-form" onSubmit={handleSubmit}>
          <label>
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

          <label>
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
                  {servicePrice.name} · R$ {servicePrice.price.toFixed(2)}
                </option>
              ))}
            </select>
          </label>

          <label>
            <span>Data e horário</span>
            <input
              type="datetime-local"
              value={scheduledAt}
              onChange={(event) => setScheduledAt(event.target.value)}
              required
            />
          </label>

          {error && <p className="error-message">{error}</p>}
          {success && <p className="success-message">{success}</p>}

          <button type="submit" className="primary-btn" disabled={loading}>
            {loading ? 'Agendando...' : 'Agendar'}
          </button>
        </form>
      )}
    </section>
  )
}
