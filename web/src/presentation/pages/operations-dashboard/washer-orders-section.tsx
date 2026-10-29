import { useCallback, useState } from 'react'
import { UiIcon } from '@/presentation/components/ui-icon'
import type { ServiceOrderModel } from '@/domain/models/service-order-model'
import type { Operations, PauseReason } from '@/domain/usecases/operations'
import { BUSINESS_TIME_ZONE } from '@/main/config/business-time'

const STATUS_LABELS: Record<ServiceOrderModel['status'], string> = {
  SCHEDULED: 'Agendado',
  WAITING_IN_YARD: 'Aguardando no pátio',
  WASHING: 'Em lavagem',
  PAUSED: 'Pausada',
  READY: 'Pronto',
  DONE: 'Finalizado',
}

const PAUSE_LABELS: Record<PauseReason, string> = {
  LUNCH: 'Almoço',
  RESTROOM: 'Banheiro',
  OTHER: 'Outro',
}

const PAUSE_REASONS: { value: PauseReason; label: string }[] = [
  { value: 'LUNCH', label: 'Almoço' },
  { value: 'RESTROOM', label: 'Banheiro' },
  { value: 'OTHER', label: 'Outro motivo' },
]

type Props = Readonly<{
  myOrders: ServiceOrderModel[]
  operations: Operations
  busyOrderId: string | null
  setBusyOrderId: (id: string | null) => void
  setNotice: (msg: string | null) => void
  setError: (msg: string | null) => void
  refresh: () => Promise<void>
}>

export function WasherOrdersSection({
  myOrders,
  operations,
  busyOrderId,
  setBusyOrderId,
  setNotice,
  setError,
  refresh,
}: Props) {
  const [pauseReason, setPauseReason] = useState<PauseReason>('LUNCH')
  const [pauseDescription, setPauseDescription] = useState('')

  const runAction = useCallback(
    async (orderId: string, action: () => Promise<void>, successMessage: string) => {
      setBusyOrderId(orderId)
      setError(null)
      setNotice(null)
      try {
        await action()
        setNotice(successMessage)
        await refresh()
      } catch (cause) {
        setError(
          cause instanceof Error ? cause.message : 'Falha ao atualizar atendimento',
        )
      } finally {
        setBusyOrderId(null)
      }
    },
    [setBusyOrderId, setError, setNotice, refresh],
  )

  return (
    <section className="dashboard-section">
      <h2 className="section-title">
        <span className="section-icon">
          <UiIcon name="washer" />
        </span>
        Minhas lavagens
      </h2>
      {myOrders.length === 0 ? (
        <p className="empty-state">Nenhuma lavagem em andamento.</p>
      ) : (
        <ul className="order-list">
          {myOrders.map((order) => (
            <li key={order.id}>
              <span className={`status-badge status-${order.status.toLowerCase()}`}>
                {STATUS_LABELS[order.status]}
              </span>
              <span className="order-plate">OS {order.id.slice(0, 8)}</span>
              <span className="order-date">
                {new Date(order.scheduledAt).toLocaleString('pt-BR', {
                  timeZone: BUSINESS_TIME_ZONE,
                })}
              </span>
              {order.status === 'PAUSED' && order.pauseReason && (
                <span className="pause-reason">
                  Motivo: {PAUSE_LABELS[order.pauseReason]}
                </span>
              )}
              {order.status === 'WASHING' ? (
                <div className="washer-order-controls">
                  <label>
                    Motivo da pausa
                    <select
                      value={pauseReason}
                      onChange={(event) => {
                        const selectedReason = PAUSE_REASONS.find(
                          (reason) => reason.value === event.target.value,
                        )
                        if (selectedReason) {
                          setPauseReason(selectedReason.value)
                        }
                      }}
                    >
                      {PAUSE_REASONS.map((reason) => (
                        <option key={reason.value} value={reason.value}>
                          {reason.label}
                        </option>
                      ))}
                    </select>
                  </label>
                  {pauseReason === 'OTHER' && (
                    <label>
                      Descreva o motivo
                      <input
                        maxLength={200}
                        value={pauseDescription}
                        onChange={(event) => setPauseDescription(event.target.value)}
                      />
                    </label>
                  )}
                  <button
                    className="secondary-btn"
                    disabled={
                      busyOrderId === order.id ||
                      (pauseReason === 'OTHER' && !pauseDescription.trim())
                    }
                    onClick={() =>
                      void runAction(
                        order.id,
                        () =>
                          operations.pause(
                            order.id,
                            pauseReason,
                            pauseReason === 'OTHER' ? pauseDescription.trim() : undefined,
                          ),
                        `Lavagem pausada: ${PAUSE_LABELS[pauseReason]}.`,
                      )
                    }
                  >
                    <UiIcon name="pause" size={16} />
                    Pausar lavagem
                  </button>
                  <button
                    className="primary-btn"
                    disabled={busyOrderId === order.id}
                    onClick={() =>
                      void runAction(
                        order.id,
                        () => operations.complete(order.id),
                        'Lavagem concluída.',
                      )
                    }
                  >
                    <UiIcon name="check" size={16} />
                    Concluir lavagem
                  </button>
                </div>
              ) : (
                <button
                  className="primary-btn"
                  disabled={busyOrderId === order.id}
                  onClick={() =>
                    void runAction(
                      order.id,
                      () => operations.resume(order.id),
                      'Lavagem retomada.',
                    )
                  }
                >
                  <UiIcon name="play" size={16} />
                  Retomar lavagem
                </button>
              )}
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
