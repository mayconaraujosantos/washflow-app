import {
  ServiceOrderModel,
  ServiceOrderStatus,
} from '@/domain/models/service-order-model'
import { VehicleModel } from '@/domain/models/vehicle-model'

const STATUS_LABELS: Record<ServiceOrderStatus, string> = {
  SCHEDULED: 'Agendado',
  WAITING_IN_YARD: 'Aguardando no pátio',
  WASHING: 'Em lavagem',
  READY: 'Pronto para retirada',
  DONE: 'Finalizado',
}

type Props = Readonly<{
  serviceOrders: ServiceOrderModel[]
  vehicles: VehicleModel[]
  refreshing: boolean
  onRefresh: () => void
}>

export function TrackingSection({
  serviceOrders,
  vehicles,
  refreshing,
  onRefresh,
}: Props) {
  const plateByVehicleId = new Map(
    vehicles.map((vehicle) => [vehicle.id, vehicle.plate]),
  )

  return (
    <section className="dashboard-section">
      <div className="section-header">
        <h2>Meus agendamentos</h2>
        <button
          type="button"
          className="secondary-btn"
          onClick={onRefresh}
          disabled={refreshing}
        >
          {refreshing ? 'Atualizando...' : 'Atualizar'}
        </button>
      </div>

      {serviceOrders.length === 0 ? (
        <p className="empty-state">Nenhum agendamento ainda.</p>
      ) : (
        <ul className="order-list">
          {serviceOrders.map((serviceOrder) => (
            <li key={serviceOrder.id}>
              <span
                className={`status-badge status-${serviceOrder.status.toLowerCase()}`}
              >
                {STATUS_LABELS[serviceOrder.status]}
              </span>
              <span className="order-plate">
                {plateByVehicleId.get(serviceOrder.vehicleId) ?? 'Veículo'}
              </span>
              <span className="order-date">
                {new Date(serviceOrder.scheduledAt).toLocaleString('pt-BR')}
              </span>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
