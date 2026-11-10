import { useState } from 'react'
import { refundableByMethod, type SaleModel } from '@/domain/models/sale-model'
import type {
  PaymentMethod,
  RefundReason,
  SaleRefundParams,
} from '@/domain/usecases/operations'
import { PAYMENT_LABELS, REFUND_LABELS } from '@/main/config/labels'
import { UiIcon } from '@/presentation/components/ui-icon'
import { BottomSheet } from '@/presentation/components/ui/blocks'
import { formatMoney } from '@/presentation/helpers/format'

const REFUND_REASONS: RefundReason[] = [
  'CUSTOMER_COMPLAINT',
  'WRONG_CHARGE',
  'SERVICE_NOT_DONE',
  'OTHER',
]

const toInput = (value: number) => value.toFixed(2).replace('.', ',')

/** "1.234,50" or "7,5" (Brazilian) and "50.01" all read as reais. */
function parseAmount(text: string): number {
  const normalized = text.includes(',')
    ? text.replaceAll('.', '').replace(',', '.')
    : text
  return Number(normalized.trim())
}

/**
 * Total or partial refund of a quick sale: one method at a time, at most what that method still
 * has (the API checks it again under a lock).
 */
export function SaleRefundSheet({
  sale,
  onClose,
  onConfirm,
}: Readonly<{
  sale: SaleModel | null
  onClose: () => void
  onConfirm: (sale: SaleModel, params: SaleRefundParams) => void
}>) {
  const available = sale ? refundableByMethod(sale) : {}
  const methods = Object.keys(available) as PaymentMethod[]
  const [method, setMethod] = useState<PaymentMethod | undefined>(methods[0])
  const [amountText, setAmountText] = useState(
    methods[0] ? toInput(available[methods[0]] ?? 0) : '',
  )
  const [reason, setReason] = useState<RefundReason>('CUSTOMER_COMPLAINT')
  const [description, setDescription] = useState('')
  if (!sale || !method) return null

  const max = available[method] ?? 0
  const amount = parseAmount(amountText)
  const invalidAmount =
    !Number.isFinite(amount) ||
    amount <= 0 ||
    Math.round(amount * 100) / 100 !== amount
  const aboveMax = !invalidAmount && amount > max
  const needsDescription = reason === 'OTHER' && !description.trim()

  return (
    <BottomSheet open title="Estornar venda" onClose={onClose}>
      <div className="client-chip">
        <span>{sale.customerName ?? 'Venda balcão'}</span>
        <strong>{formatMoney(sale.total)}</strong>
      </div>
      <div className="field-row">
        <label className="field">
          <span>Devolver em</span>
          <select
            value={method}
            onChange={(event) => {
              const next = event.target.value as PaymentMethod
              setMethod(next)
              setAmountText(toInput(available[next] ?? 0))
            }}
          >
            {methods.map((option) => (
              <option key={option} value={option}>
                {PAYMENT_LABELS[option]}
              </option>
            ))}
          </select>
        </label>
        <label className="field">
          <span>Valor a devolver</span>
          <input
            className="input"
            inputMode="decimal"
            value={amountText}
            onChange={(event) => setAmountText(event.target.value)}
          />
        </label>
      </div>
      {aboveMax && (
        <p className="error-message" role="alert">
          {`Máximo em ${PAYMENT_LABELS[method]}: ${formatMoney(max)}`}
        </p>
      )}
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
      <button
        type="button"
        className="btn btn-danger btn-block"
        disabled={invalidAmount || aboveMax || needsDescription}
        onClick={() =>
          onConfirm(sale, {
            method,
            amount,
            reason,
            description: reason === 'OTHER' ? description.trim() : undefined,
          })
        }
      >
        <UiIcon name="money" size={18} />
        Confirmar estorno
      </button>
    </BottomSheet>
  )
}
