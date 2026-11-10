import { useState } from 'react'
import type { ServiceOrderModel } from '@/domain/models/service-order-model'
import type { CancelReason } from '@/domain/usecases/operations'
import { CANCEL_LABELS } from '@/main/config/labels'
import { UiIcon } from '@/presentation/components/ui-icon'
import { BottomSheet } from '@/presentation/components/ui/blocks'

const CANCEL_REASONS: CancelReason[] = [
  'CUSTOMER_GAVE_UP',
  'NO_SHOW',
  'REGISTERED_BY_MISTAKE',
  'OTHER',
]

/** Reason for cancelling an order that never reached washing (always required). */
export function CancelSheet({
  order,
  onClose,
  onConfirm,
}: Readonly<{
  order: ServiceOrderModel | null
  onClose: () => void
  onConfirm: (
    order: ServiceOrderModel,
    reason: CancelReason,
    description?: string,
  ) => void
}>) {
  const [reason, setReason] = useState<CancelReason>('CUSTOMER_GAVE_UP')
  const [description, setDescription] = useState('')
  if (!order) return null
  const needsDescription = reason === 'OTHER' && !description.trim()

  return (
    <BottomSheet open title="Cancelar atendimento" onClose={onClose}>
      <div className="client-chip">
        <span className="plate">{order.details?.vehiclePlate ?? '—'}</span>
        <span className="faint">{order.details?.serviceName}</span>
      </div>
      <p className="faint">
        A vaga é liberada e o atendimento fica no histórico como cancelado.
      </p>
      <div role="group" aria-label="Motivo do cancelamento" className="form">
        {CANCEL_REASONS.map((option) => (
          <button
            key={option}
            type="button"
            className="sheet-option"
            aria-pressed={reason === option}
            onClick={() => setReason(option)}
          >
            <UiIcon name={reason === option ? 'check' : 'close'} />
            {CANCEL_LABELS[option]}
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
        disabled={needsDescription}
        onClick={() =>
          onConfirm(
            order,
            reason,
            reason === 'OTHER' ? description.trim() : undefined,
          )
        }
      >
        <UiIcon name="trash" size={18} />
        Cancelar atendimento
      </button>
    </BottomSheet>
  )
}
