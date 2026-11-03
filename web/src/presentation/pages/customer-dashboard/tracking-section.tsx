import { ServiceOrderModel } from '@/domain/models/service-order-model'
import { VehicleModel } from '@/domain/models/vehicle-model'
import {
  PAUSE_LABELS_LOWER,
  STATUS_LABELS_DETAILED,
} from '@/main/config/labels'
import { UiIcon } from '@/presentation/components/ui-icon'
import { EmptyState, Notice } from '@/presentation/components/ui/blocks'
import { formatDateTime } from '@/presentation/helpers/format'

type Props = Readonly<{
  serviceOrders: ServiceOrderModel[]
  vehicles: VehicleModel[]
  refreshing: boolean
  statusNotice: string | null
  onRefresh: () => void
}>

export function TrackingSection({
  serviceOrders,
  vehicles,
  refreshing,
  statusNotice,
  onRefresh,
}: Props) {
  const plateByVehicleId = new Map(
    vehicles.map((vehicle) => [vehicle.id, vehicle.plate]),
  )

  return (
    <section className="form">
      <div className="section-header">
        <h2>Meus agendamentos</h2>
        <button
          type="button"
          className="btn btn-outline btn-small"
          onClick={onRefresh}
          disabled={refreshing}
        >
          <UiIcon name="refresh" size={16} />
          {refreshing ? 'Atualizando...' : 'Atualizar'}
        </button>
      </div>
      <Notice tone="success">{statusNotice}</Notice>

      {serviceOrders.length === 0 ? (
        <EmptyState icon="calendar" title="Nenhum agendamento ainda." />
      ) : (
        <ul className="list">
          {serviceOrders.map((serviceOrder) => (
            <li
              key={serviceOrder.id}
              className="order-card"
              data-status={serviceOrder.status}
            >
              <div className="order-head">
                <span className="plate">
                  {serviceOrder.details?.vehiclePlate ??
                    plateByVehicleId.get(serviceOrder.vehicleId) ??
                    'Veículo'}
                </span>
                <span
                  className={`status-badge status-${serviceOrder.status.toLowerCase()}`}
                >
                  {STATUS_LABELS_DETAILED[serviceOrder.status]}
                </span>
              </div>
              <div className="order-meta">
                {serviceOrder.details?.serviceName && (
                  <span>
                    <UiIcon name="tag" size={15} />
                    {serviceOrder.details.serviceName}
                  </span>
                )}
                <span>
                  <UiIcon name="clock" size={15} />
                  {formatDateTime(serviceOrder.scheduledAt)}
                </span>
              </div>
              {serviceOrder.status === 'PAUSED' && (
                <p className="notice notice-warning">
                  <UiIcon name="clock" size={17} />
                  Lavagem pausada
                  {serviceOrder.pauseReason
                    ? `: ${PAUSE_LABELS_LOWER[serviceOrder.pauseReason]}.`
                    : '.'}{' '}
                  Será retomada em breve.
                </p>
              )}
              {serviceOrder.status === 'READY' && (
                <p
                  className="notice notice-success"
                  role="status"
                  aria-live="polite"
                >
                  <UiIcon name="check" size={18} />
                  Seu veículo está pronto para retirada. Dirija-se ao gerente
                  para concluir o atendimento.
                </p>
              )}
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
