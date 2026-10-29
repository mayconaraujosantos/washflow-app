import { useCallback, useState } from 'react'
import { UiIcon } from '@/presentation/components/ui-icon'
import type { ServiceOrderModel } from '@/domain/models/service-order-model'
import type { ServicePriceModel } from '@/domain/models/service-price-model'
import type { Operations, PaymentMethod } from '@/domain/usecases/operations'
import { BUSINESS_TIME_ZONE } from '@/main/config/business-time'

const STATUS_LABELS: Record<ServiceOrderModel['status'], string> = {
  SCHEDULED: 'Agendado',
  WAITING_IN_YARD: 'Aguardando no pátio',
  WASHING: 'Em lavagem',
  PAUSED: 'Pausada',
  READY: 'Pronto',
  DONE: 'Finalizado',
}

const PAUSE_LABELS: Record<string, string> = {
  LUNCH: 'Almoço',
  RESTROOM: 'Banheiro',
  OTHER: 'Outro',
}

const PAYMENT_METHODS: { value: PaymentMethod; label: string }[] = [
  { value: 'CASH', label: 'Dinheiro' },
  { value: 'PIX', label: 'Pix' },
  { value: 'DEBIT_CARD', label: 'Cartão de débito' },
  { value: 'CREDIT_CARD', label: 'Cartão de crédito' },
]

type Props = Readonly<{
  yard: ServiceOrderModel[]
  servicePrices: ServicePriceModel[]
  operations: Operations
  busyOrderId: string | null
  setBusyOrderId: (id: string | null) => void
  setNotice: (msg: string | null) => void
  setError: (msg: string | null) => void
  refresh: () => Promise<void>
}>

export function ManagerYardSection({
  yard,
  servicePrices,
  operations,
  busyOrderId,
  setBusyOrderId,
  setNotice,
  setError,
  refresh,
}: Props) {
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('PIX')

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
      <div className="section-header">
        <h2 className="section-title">
          <span className="section-icon">
            <UiIcon name="clipboard" />
          </span>
          Pátio geral
        </h2>
        <label>
          <span>Forma de pagamento ao finalizar</span>
          <select
            value={paymentMethod}
            onChange={(event) => setPaymentMethod(event.target.value as PaymentMethod)}
          >
            {PAYMENT_METHODS.map((method) => (
              <option key={method.value} value={method.value}>
                {method.label}
              </option>
            ))}
          </select>
        </label>
      </div>
      {yard.length === 0 ? (
        <p className="empty-state">Nenhum atendimento ativo no pátio.</p>
      ) : (
        <ul className="order-list">
          {yard.map((order) => (
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
              {order.status === 'READY' && (
                <span>
                  Valor: R${' '}
                  {servicePrices
                    .find((price) => price.id === order.servicePriceId)
                    ?.price.toFixed(2) ?? '—'}
                </span>
              )}
              {order.status === 'SCHEDULED' ? (
                <button
                  className="primary-btn"
                  disabled={busyOrderId === order.id}
                  onClick={() =>
                    void runAction(
                      order.id,
                      () => operations.checkIn(order.id),
                      'Veículo recebido no pátio.',
                    )
                  }
                >
                  <UiIcon name="check" size={16} />
                  Receber no pátio
                </button>
              ) : order.status === 'READY' ? (
                <button
                  className="primary-btn"
                  disabled={busyOrderId === order.id}
                  onClick={() =>
                    void runAction(
                      order.id,
                      () => operations.finalize(order.id, paymentMethod),
                      'Pagamento registrado e atendimento finalizado.',
                    )
                  }
                >
                  <UiIcon name="check" size={16} />
                  Registrar pagamento e finalizar
                </button>
              ) : undefined}
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
