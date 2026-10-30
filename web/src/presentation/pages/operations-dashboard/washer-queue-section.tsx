import { useCallback } from 'react'
import { UiIcon } from '@/presentation/components/ui-icon'
import type { ServiceOrderModel } from '@/domain/models/service-order-model'
import type { Operations } from '@/domain/usecases/operations'
import { BUSINESS_TIME_ZONE } from '@/main/config/business-time'
import { STATUS_LABELS } from '@/main/config/labels'

type Props = Readonly<{
  waiting: ServiceOrderModel[]
  operations: Operations
  userId: string
  busyOrderId: string | null
  setBusyOrderId: (id: string | null) => void
  setNotice: (msg: string | null) => void
  setError: (msg: string | null) => void
  refresh: () => Promise<void>
}>

export function WasherQueueSection({
  waiting,
  operations,
  userId,
  busyOrderId,
  setBusyOrderId,
  setNotice,
  setError,
  refresh,
}: Props) {
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
          <UiIcon name="clipboard" />
        </span>
        Fila do pátio
      </h2>
      {waiting.length === 0 ? (
        <p className="empty-state">Nenhum veículo aguardando.</p>
      ) : (
        <ul className="order-list">
          {waiting.map((order) => (
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
              <button
                className="primary-btn"
                disabled={busyOrderId === order.id}
                onClick={() =>
                  void runAction(
                    order.id,
                    () => operations.start(order.id, userId),
                    'Lavagem iniciada.',
                  )
                }
              >
                Iniciar lavagem
              </button>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
