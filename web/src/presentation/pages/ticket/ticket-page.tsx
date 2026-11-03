import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import type { TicketModel } from '@/domain/models/access-model'
import type { ServiceOrderStatus } from '@/domain/models/service-order-model'
import type { Access } from '@/domain/usecases/access'
import { PAUSE_LABELS_LOWER } from '@/main/config/labels'
import { UiIcon, type UiIconName } from '@/presentation/components/ui-icon'
import { BrandLogo } from '@/presentation/components/ui/app-chrome'
import { Notice } from '@/presentation/components/ui/blocks'
import { errorMessage, formatDateTime } from '@/presentation/helpers/format'

const REFRESH_MS = 5000

const STEPS: {
  label: string
  icon: UiIconName
  statuses: ServiceOrderStatus[]
}[] = [
  {
    label: 'Recebido',
    icon: 'entry',
    statuses: ['SCHEDULED', 'WAITING_IN_YARD'],
  },
  { label: 'Em lavagem', icon: 'washer', statuses: ['WASHING', 'PAUSED'] },
  { label: 'Pronto', icon: 'check', statuses: ['READY'] },
  { label: 'Entregue', icon: 'exit', statuses: ['DONE'] },
]

const HEADLINES: Record<ServiceOrderStatus, string> = {
  SCHEDULED: 'Agendado — aguardando a chegada do veículo',
  WAITING_IN_YARD: 'No pátio, aguardando um lavador',
  WASHING: 'Seu veículo está sendo lavado',
  PAUSED: 'Lavagem pausada por instantes',
  READY: 'Pronto para retirada!',
  DONE: 'Atendimento finalizado. Obrigado!',
}

type Props = Readonly<{ access: Access }>

/** Public order tracking from the ticket QR: no login, refreshes every 5s. */
export function TicketPage({ access }: Props) {
  const { token = '' } = useParams()
  const [ticket, setTicket] = useState<TicketModel | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    const load = () =>
      access
        .loadTicket(token)
        .then((loaded) => {
          if (!active) return
          setTicket(loaded)
          setError(null)
        })
        .catch((cause) => {
          if (active) setError(errorMessage(cause, 'Ticket não encontrado'))
        })
    void load()
    const timer = window.setInterval(() => void load(), REFRESH_MS)
    return () => {
      active = false
      window.clearInterval(timer)
    }
  }, [access, token])

  const current = ticket
    ? STEPS.findIndex((step) => step.statuses.includes(ticket.status))
    : -1

  return (
    <main className="auth-shell">
      <h1 className="auth-brand">
        <BrandLogo size="lg" />
      </h1>
      {ticket && <p className="role-shop">{ticket.establishmentName}</p>}
      <section className="card ticket-card" aria-live="polite">
        {!ticket ? (
          <Notice tone={error ? 'error' : 'info'}>
            {error ?? 'Carregando seu ticket...'}
          </Notice>
        ) : (
          <>
            <div className="order-head">
              <div>
                <span className="plate">{ticket.vehiclePlate}</span>
                {ticket.vehicleModel && (
                  <p className="muted">{ticket.vehicleModel}</p>
                )}
              </div>
              <span className="faint">{ticket.serviceName}</span>
            </div>
            <h2>{HEADLINES[ticket.status]}</h2>
            {ticket.status === 'PAUSED' && ticket.pauseReason && (
              <p className="faint">
                Motivo: {PAUSE_LABELS_LOWER[ticket.pauseReason]}. Será retomada
                em breve.
              </p>
            )}
            <ol className="ticket-steps">
              {STEPS.map((step, index) => (
                <li
                  key={step.label}
                  className={
                    index < current
                      ? 'is-done'
                      : index === current
                        ? 'is-current'
                        : ''
                  }
                  aria-current={index === current ? 'step' : undefined}
                >
                  <span className="ticket-step-dot">
                    <UiIcon
                      name={index < current ? 'check' : step.icon}
                      size={18}
                    />
                  </span>
                  {step.label}
                </li>
              ))}
            </ol>
            {ticket.status === 'READY' && (
              <p className="notice notice-success">
                <UiIcon name="check" size={18} />
                Passe no balcão para pagar e retirar o veículo.
              </p>
            )}
            <div className="order-meta">
              {ticket.expectedDeliveryAt && (
                <span>
                  <UiIcon name="calendar" size={15} />
                  Previsão {formatDateTime(ticket.expectedDeliveryAt)}
                </span>
              )}
              <span>
                <UiIcon name="refresh" size={15} />
                Atualizado {formatDateTime(ticket.updatedAt)}
              </span>
            </div>
            <Notice tone="error">{error}</Notice>
          </>
        )}
      </section>
      <a href="/login/cliente" className="auth-back">
        Quer agendar e acompanhar pelo app? Entre como cliente
      </a>
    </main>
  )
}
