import { useState } from 'react'
import type { ServiceOrderModel } from '@/domain/models/service-order-model'
import type {
  PaymentMethod,
  RefundParams,
  RefundReason,
} from '@/domain/usecases/operations'
import { PAYMENT_LABELS, REFUND_LABELS } from '@/main/config/labels'
import { UiIcon } from '@/presentation/components/ui-icon'
import { BottomSheet } from '@/presentation/components/ui/blocks'
import { formatMoney } from '@/presentation/helpers/format'
import { PAYMENT_METHODS } from './manager-context'

const REFUND_REASONS: RefundReason[] = [
  'CUSTOMER_COMPLAINT',
  'WRONG_CHARGE',
  'SERVICE_NOT_DONE',
  'OTHER',
]

/**
 * Full refund of a finalized order: required reason, refund method (the one paid by default) and
 * the washer's commission kept unless the manager chooses to reverse it.
 */
export function RefundSheet({
  order,
  amount,
  onClose,
  onConfirm,
}: Readonly<{
  order: ServiceOrderModel | null
  amount?: number
  onClose: () => void
  onConfirm: (order: ServiceOrderModel, params: RefundParams) => void
}>) {
  const [reason, setReason] = useState<RefundReason>('CUSTOMER_COMPLAINT')
  const [description, setDescription] = useState('')
  const [method, setMethod] = useState<PaymentMethod | ''>('')
  const [reverseCommission, setReverseCommission] = useState(false)
  if (!order) return null
  const needsDescription = reason === 'OTHER' && !description.trim()

  return (
    <BottomSheet open title="Estornar pagamento" onClose={onClose}>
      <div className="client-chip">
        <span className="plate">{order.details?.vehiclePlate ?? '—'}</span>
        <strong>{formatMoney(amount)}</strong>
      </div>
      <p className="faint">
        Devolve o valor pago, lançado hoje. O relatório dos dias anteriores não
        muda.
      </p>
      <div role="group" aria-label="Motivo do estorno" className="form">
        {REFUND_REASONS.map((option) => (
          <button
            key={option}
            type="button"
            className="sheet-option"
            aria-pressed={reason === option}
            onClick={() => setReason(option)}
          >
            <UiIcon name={reason === option ? 'check' : 'money'} />
            {REFUND_LABELS[option]}
          </button>
        ))}
      </div>
      {reason === 'OTHER' && (
        <label className="field">
          <span>Descreva o motivo</span>
          <input
            className="input"
            maxLength={200}
            value={description}
            onChange={(event) => setDescription(event.target.value)}
          />
        </label>
      )}
      <label className="field">
        <span>Devolver em</span>
        <select
          value={method}
          onChange={(event) =>
            setMethod(event.target.value as PaymentMethod | '')
          }
        >
          <option value="">Mesma forma do pagamento</option>
          {PAYMENT_METHODS.map((option) => (
            <option key={option} value={option}>
              {PAYMENT_LABELS[option]}
            </option>
          ))}
        </select>
      </label>
      <label className="check">
        <input
          type="checkbox"
          checked={reverseCommission}
          onChange={(event) => setReverseCommission(event.target.checked)}
        />
        Estornar a comissão do lavador
      </label>
      <p className="faint">
        Por padrão o lavador mantém a comissão: ele fez o serviço. Confirme com
        o contador antes de descontar.
      </p>
      <button
        type="button"
        className="btn btn-danger btn-block"
        disabled={needsDescription}
        onClick={() =>
          onConfirm(order, {
            reason,
            description: reason === 'OTHER' ? description.trim() : undefined,
            refundMethod: method || undefined,
            reverseCommission,
          })
        }
      >
        <UiIcon name="money" size={18} />
        Confirmar estorno
      </button>
    </BottomSheet>
  )
}
