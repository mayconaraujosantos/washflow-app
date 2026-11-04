import { useState } from 'react'
import type { ChecklistType } from '@/domain/models/feature-models'
import type { ServiceOrderModel } from '@/domain/models/service-order-model'
import type { PaymentMethod } from '@/domain/usecases/operations'
import { PAYMENT_LABELS } from '@/main/config/labels'
import { ChecklistModal } from '@/presentation/components/checklist-modal'
import { UiIcon } from '@/presentation/components/ui-icon'
import { ScreenLayout } from '@/presentation/components/ui/app-chrome'
import {
  BottomSheet,
  EmptyState,
  OrderCard,
} from '@/presentation/components/ui/blocks'
import { errorMessage, formatMoney } from '@/presentation/helpers/format'
import { useLocation, useNavigate } from 'react-router-dom'
import { TicketSheet } from './access-sheets'
import { MANAGER_NAV, PAYMENT_METHODS, useManager } from './manager-context'

const GROUPS: { title: string; statuses: ServiceOrderModel['status'][] }[] = [
  { title: 'Prontos para retirada', statuses: ['READY'] },
  { title: 'Em lavagem', statuses: ['WASHING', 'PAUSED'] },
  { title: 'Aguardando lavador', statuses: ['WAITING_IN_YARD'] },
  { title: 'Agendados', statuses: ['SCHEDULED'] },
]

type Props = Readonly<{ mode: 'yard' | 'exit' }>

export function YardScreen({ mode }: Props) {
  const { yard, operations, features, refresh, notify, openMenu } = useManager()
  const [busyId, setBusyId] = useState<string | null>(null)
  const [paying, setPaying] = useState<ServiceOrderModel | null>(null)
  const location = useLocation()
  const navigate = useNavigate()
  const [ticketFor, setTicketFor] = useState<ServiceOrderModel | null>(null)
  // Right after registering an entry the counter hands the customer a ticket.
  const entryState = location.state as {
    ticketOrderId?: string
    checklist?: boolean
  } | null
  const ticketOrderId = entryState?.ticketOrderId
  const pendingTicket = ticketOrderId
    ? (yard.find((order) => order.id === ticketOrderId) ?? null)
    : null
  // "Check-list" ticked on the entry form: entry checklist first, then the ticket.
  const [entryChecklistDone, setEntryChecklistDone] = useState(false)
  const entryChecklistOpen = Boolean(
    pendingTicket && entryState?.checklist && !entryChecklistDone,
  )
  const [checklist, setChecklist] = useState<{
    orderId: string
    type: ChecklistType
  } | null>(null)

  const run = async (
    order: ServiceOrderModel,
    action: () => Promise<void>,
    message: string,
  ) => {
    setBusyId(order.id)
    try {
      await action()
      notify('success', message)
      await refresh()
    } catch (cause) {
      notify('error', errorMessage(cause, 'Falha ao atualizar atendimento'))
    } finally {
      setBusyId(null)
    }
  }

  const groups =
    mode === 'exit'
      ? [{ title: 'Prontos para retirada', statuses: ['READY'] }]
      : GROUPS
  const visible = groups
    .map((group) => ({
      ...group,
      orders: yard.filter((order) => group.statuses.includes(order.status)),
    }))
    .filter((group) => group.orders.length > 0)

  const plateOf = (order: ServiceOrderModel) =>
    order.details?.vehiclePlate ?? 'veículo'

  const renderActions = (order: ServiceOrderModel) => {
    const busy = busyId === order.id
    switch (order.status) {
      case 'SCHEDULED':
        return (
          <button
            type="button"
            className="btn btn-brand"
            disabled={busy}
            onClick={() =>
              void run(
                order,
                () => operations.checkIn(order.id),
                `${plateOf(order)} recebido no pátio.`,
              )
            }
          >
            <UiIcon name="entry" size={18} />
            Receber no pátio
          </button>
        )
      case 'WAITING_IN_YARD':
        return (
          <button
            type="button"
            className="btn btn-outline"
            onClick={() => setChecklist({ orderId: order.id, type: 'ENTRY' })}
          >
            <UiIcon name="clipboard" size={18} />
            Checklist de entrada
          </button>
        )
      case 'READY':
        return (
          <>
            <button
              type="button"
              className="btn btn-outline"
              onClick={() => setChecklist({ orderId: order.id, type: 'EXIT' })}
            >
              <UiIcon name="clipboard" size={18} />
              Checklist
            </button>
            <button
              type="button"
              className="btn btn-accent"
              disabled={busy}
              onClick={() => setPaying(order)}
            >
              <UiIcon name="money" size={18} />
              Receber e finalizar
            </button>
          </>
        )
      default:
        return null
    }
  }

  return (
    <ScreenLayout
      title={mode === 'exit' ? 'Saída de veículos' : undefined}
      backTo={mode === 'exit' ? '/gerente' : undefined}
      onMenu={mode === 'yard' ? openMenu : undefined}
      nav={mode === 'yard' ? MANAGER_NAV : undefined}
    >
      {visible.length === 0 ? (
        <EmptyState
          icon={mode === 'exit' ? 'exit' : 'parking'}
          title={
            mode === 'exit'
              ? 'Nenhum veículo pronto para retirada.'
              : 'Pátio vazio! Todos os carros foram finalizados.'
          }
          accent={
            mode === 'exit'
              ? 'Os veículos aparecem aqui quando o lavador conclui.'
              : 'Registre uma entrada pela tela inicial.'
          }
        />
      ) : (
        visible.map((group) => (
          <section key={group.title} className="form">
            <h2>
              {group.title}{' '}
              <span className="faint">({group.orders.length})</span>
            </h2>
            <ul className="list">
              {group.orders.map((order) => (
                <OrderCard key={order.id} order={order} showWasher>
                  {renderActions(order)}
                  <button
                    type="button"
                    className="btn btn-ghost"
                    onClick={() => setTicketFor(order)}
                  >
                    <UiIcon name="qr" size={18} />
                    Ticket
                  </button>
                </OrderCard>
              ))}
            </ul>
          </section>
        ))
      )}

      <TicketSheet
        order={ticketFor ?? (entryChecklistOpen ? null : pendingTicket)}
        onClose={() => {
          setTicketFor(null)
          if (ticketOrderId) navigate('.', { replace: true, state: null })
        }}
      />
      <PaymentSheet
        order={paying}
        onClose={() => setPaying(null)}
        onConfirm={(order, method) => {
          setPaying(null)
          void run(
            order,
            () => operations.finalize(order.id, method),
            `Pagamento registrado. ${plateOf(order)} finalizado.`,
          )
        }}
      />
      {entryChecklistOpen && pendingTicket && (
        <ChecklistModal
          features={features}
          serviceOrderId={pendingTicket.id}
          type="ENTRY"
          onClose={() => setEntryChecklistDone(true)}
        />
      )}
      {checklist && (
        <ChecklistModal
          features={features}
          serviceOrderId={checklist.orderId}
          type={checklist.type}
          onClose={() => setChecklist(null)}
        />
      )}
    </ScreenLayout>
  )
}

function PaymentSheet({
  order,
  onClose,
  onConfirm,
}: Readonly<{
  order: ServiceOrderModel | null
  onClose: () => void
  onConfirm: (order: ServiceOrderModel, method: PaymentMethod) => void
}>) {
  const { servicePrices } = useManager()
  const [method, setMethod] = useState<PaymentMethod>('PIX')
  if (!order) return null
  const price = servicePrices.find((item) => item.id === order.servicePriceId)

  return (
    <BottomSheet open title="Receber pagamento" onClose={onClose}>
      <div className="client-chip">
        <span className="plate">{order.details?.vehiclePlate ?? '—'}</span>
        <strong>{formatMoney(price?.price)}</strong>
      </div>
      <p className="faint">
        {order.details?.serviceName ?? price?.name} · valor do catálogo
      </p>
      <div role="group" aria-label="Forma de pagamento" className="form">
        {PAYMENT_METHODS.map((option) => (
          <button
            key={option}
            type="button"
            className="sheet-option"
            aria-pressed={method === option}
            onClick={() => setMethod(option)}
          >
            <UiIcon name={method === option ? 'check' : 'money'} />
            {PAYMENT_LABELS[option]}
          </button>
        ))}
      </div>
      <button
        type="button"
        className="btn btn-accent btn-block"
        onClick={() => onConfirm(order, method)}
      >
        Confirmar e finalizar
      </button>
    </BottomSheet>
  )
}
