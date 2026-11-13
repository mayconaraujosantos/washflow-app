import { useEffect, useState } from 'react'
import type { ServiceOrderModel } from '@/domain/models/service-order-model'
import type { WasherStatusModel } from '@/domain/models/washer-status-model'
import { UiIcon } from '@/presentation/components/ui-icon'
import { BottomSheet, Notice } from '@/presentation/components/ui/blocks'
import { errorMessage } from '@/presentation/helpers/format'
import {
  reservedText,
  washerStateText,
} from '@/presentation/helpers/washer-status'
import { useManager } from './manager-context'

const SHARED_QUEUE = ''

/**
 * Who washes this car: the shared queue (any washer, first come) or one washer, with the free
 * ones first. Used to receive a car in the yard and to change the washer before the wash starts.
 */
export function WasherPickerSheet({
  order,
  title,
  confirmLabel,
  onClose,
  onConfirm,
}: Readonly<{
  order: ServiceOrderModel | null
  title: string
  confirmLabel: string
  onClose: () => void
  onConfirm: (order: ServiceOrderModel, washerId: string | null) => void
}>) {
  const { operations } = useManager()
  const [washers, setWashers] = useState<WasherStatusModel[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  // The parent mounts one sheet per car (key), so the state starts from this car.
  const [choice, setChoice] = useState(order?.washerId ?? SHARED_QUEUE)
  const open = order !== null

  useEffect(() => {
    if (!open) return
    let active = true
    operations
      .loadWasherBoard()
      .then((board) => {
        if (active) setWashers(board)
      })
      .catch((cause: unknown) => {
        if (active) {
          setError(
            errorMessage(cause, 'Não foi possível carregar os lavadores'),
          )
        }
      })
    return () => {
      active = false
    }
  }, [open, operations])

  if (!order) return null

  const option = (value: string, label: string, hint?: string) => (
    <button
      key={value || 'shared'}
      type="button"
      className="sheet-option"
      aria-pressed={choice === value}
      onClick={() => setChoice(value)}
    >
      <UiIcon name={choice === value ? 'check' : 'washer'} />
      <span>
        {label}
        {hint && (
          <>
            <br />
            <small className="faint">{hint}</small>
          </>
        )}
      </span>
    </button>
  )

  return (
    <BottomSheet open title={title} onClose={onClose}>
      <div className="client-chip">
        <span className="plate">{order.details?.vehiclePlate ?? '—'}</span>
        <span className="faint">{order.details?.serviceName}</span>
      </div>
      <div role="group" aria-label="Quem vai lavar" className="form">
        {option(
          SHARED_QUEUE,
          'Fila geral',
          'O primeiro lavador livre pega o carro.',
        )}
        {washers?.map((washer) =>
          option(
            washer.id,
            washer.name,
            [washerStateText(washer), reservedText(washer)]
              .filter(Boolean)
              .join(' · '),
          ),
        )}
      </div>
      {washers === null && !error && <p className="muted">Carregando...</p>}
      {washers?.length === 0 && (
        <Notice tone="warning">
          Nenhum lavador ativo. Cadastre em Equipe e acessos.
        </Notice>
      )}
      <Notice tone="error">{error}</Notice>
      <button
        type="button"
        className="btn btn-brand btn-block"
        onClick={() =>
          onConfirm(order, choice === SHARED_QUEUE ? null : choice)
        }
      >
        <UiIcon name="check" size={18} />
        {confirmLabel}
      </button>
    </BottomSheet>
  )
}
