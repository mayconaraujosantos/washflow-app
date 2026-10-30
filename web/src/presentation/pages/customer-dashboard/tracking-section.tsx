import { ServiceOrderModel } from '@/domain/models/service-order-model'
import { VehicleModel } from '@/domain/models/vehicle-model'
import { BUSINESS_TIME_ZONE } from '@/main/config/business-time'
import { PAUSE_LABELS_LOWER, STATUS_LABELS_DETAILED } from '@/main/config/labels'
import { UiIcon } from '@/presentation/components/ui-icon'

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
    <section className="dashboard-section">
      <div className="section-header">
        <h2 className="section-title">
          <span className="section-icon">
            <UiIcon name="clipboard" />
          </span>
          Meus agendamentos
        </h2>
        <button
          type="button"
          className="secondary-btn"
          onClick={onRefresh}
          disabled={refreshing}
        >
          <UiIcon name="refresh" size={16} />
          {refreshing ? 'Atualizando...' : 'Atualizar'}
        </button>
      </div>
      {statusNotice && (
        <p className="success-message" role="status" aria-live="polite">
          {statusNotice}
        </p>
      )}

      {serviceOrders.length === 0 ? (
        <p className="empty-state">Nenhum agendamento ainda.</p>
      ) : (
        <ul className="order-list">
          {serviceOrders.map((serviceOrder) => (
            <li key={serviceOrder.id}>
              <span
                className={`status-badge status-${serviceOrder.status.toLowerCase()}`}
              >
                {STATUS_LABELS_DETAILED[serviceOrder.status]}
              </span>
              <span className="order-plate">
                {plateByVehicleId.get(serviceOrder.vehicleId) ?? 'Veículo'}
              </span>
              <span className="order-date">
                {new Date(serviceOrder.scheduledAt).toLocaleString('pt-BR', {
                  timeZone: BUSINESS_TIME_ZONE,
                })}
              </span>
              {serviceOrder.status === 'PAUSED' && (
                <p className="pause-notice">
                  <UiIcon name="clock" size={17} />
                  Lavagem pausada
                  {serviceOrder.pauseReason
                    ? `: ${PAUSE_LABELS_LOWER[serviceOrder.pauseReason]}.`
                    : '.'}{' '}
                  Será retomada em breve.
                </p>
              )}
              {serviceOrder.status === 'READY' && (
                <p className="pickup-notice" role="status" aria-live="polite">
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
