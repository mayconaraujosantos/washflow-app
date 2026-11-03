import { useEffect, useState } from 'react'
import { QRCodeSVG } from 'qrcode.react'
import type { WasherBadgeModel } from '@/domain/models/access-model'
import type { ServiceOrderModel } from '@/domain/models/service-order-model'
import type { UserModel } from '@/domain/models/user-model'
import { getReachableOrigin } from '@/main/config/reachable-origin'
import { UiIcon } from '@/presentation/components/ui-icon'
import { BottomSheet, Notice } from '@/presentation/components/ui/blocks'
import { errorMessage } from '@/presentation/helpers/format'
import { useManager } from './manager-context'

/**
 * Issues a new badge for the washer and shows it once: the API keeps only
 * hashes, and issuing again invalidates the previous PIN and QR.
 */
export function BadgeSheet({
  washer,
  onClose,
}: Readonly<{ washer: UserModel | null; onClose: () => void }>) {
  const { access } = useManager()
  const [badge, setBadge] = useState<WasherBadgeModel | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!washer) return
    let active = true
    access
      .issueWasherBadge(washer.id)
      .then((issued) => {
        if (active) setBadge(issued)
      })
      .catch((cause) => {
        if (active)
          setError(errorMessage(cause, 'Não foi possível gerar o crachá'))
      })
    return () => {
      active = false
    }
  }, [access, washer])

  if (!washer) return null
  const link = badge
    ? `${getReachableOrigin()}/login/lavador?cracha=${encodeURIComponent(badge.badgeToken)}`
    : ''

  return (
    <BottomSheet open title="Crachá do lavador" onClose={onClose}>
      <Notice tone="error">{error}</Notice>
      {badge ? (
        <>
          <div className="printable badge-card">
            <strong>{washer.name}</strong>
            <span className="faint">Lavador · {washer.phone}</span>
            <div className="qr-frame">
              <QRCodeSVG value={link} size={200} marginSize={2} />
            </div>
            <span className="faint">PIN</span>
            <span className="badge-pin">{badge.pin}</span>
          </div>
          <Notice tone="warning">
            Anote ou imprima agora: o PIN e o QR aparecem só uma vez. Gerar de
            novo invalida este crachá.
          </Notice>
          <button
            type="button"
            className="btn btn-accent btn-block"
            onClick={() => window.print()}
          >
            Imprimir crachá
          </button>
        </>
      ) : (
        !error && <p className="muted">Gerando crachá...</p>
      )}
    </BottomSheet>
  )
}

/** The order's public ticket: QR to show/print, link to copy or send by WhatsApp. */
export function TicketSheet({
  order,
  onClose,
}: Readonly<{ order: ServiceOrderModel | null; onClose: () => void }>) {
  const { access } = useManager()
  const [token, setToken] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [copied, setCopied] = useState(false)

  useEffect(() => {
    if (!order) return
    let active = true
    access
      .issueTicket(order.id)
      .then((issued) => {
        if (active) setToken(issued)
      })
      .catch((cause) => {
        if (active)
          setError(errorMessage(cause, 'Não foi possível gerar o ticket'))
      })
    return () => {
      active = false
    }
  }, [access, order])

  if (!order) return null
  const link = token ? `${getReachableOrigin()}/ticket/${token}` : ''
  const phone = order.details?.customerPhone?.replace(/\D/g, '')
  const message = `Acompanhe a lavagem do seu veículo ${order.details?.vehiclePlate ?? ''}: ${link}`

  return (
    <BottomSheet open title="Ticket do cliente" onClose={onClose}>
      <Notice tone="error">{error}</Notice>
      {token ? (
        <>
          <div className="printable badge-card">
            <span className="plate">{order.details?.vehiclePlate ?? '—'}</span>
            <span className="faint">{order.details?.serviceName}</span>
            <div className="qr-frame">
              <QRCodeSVG value={link} size={200} marginSize={2} />
            </div>
            <span className="faint">
              Aponte a câmera para acompanhar sem login
            </span>
          </div>
          <div className="btn-row">
            {phone && (
              <a
                className="btn btn-brand"
                href={`https://wa.me/55${phone}?text=${encodeURIComponent(message)}`}
                target="_blank"
                rel="noreferrer"
              >
                <UiIcon name="phone" size={18} />
                Enviar no WhatsApp
              </a>
            )}
            <button
              type="button"
              className="btn btn-outline"
              onClick={() => {
                void navigator.clipboard
                  ?.writeText(link)
                  .then(() => setCopied(true))
              }}
            >
              {copied ? 'Link copiado' : 'Copiar link'}
            </button>
            <button
              type="button"
              className="btn btn-outline"
              onClick={() => window.print()}
            >
              Imprimir
            </button>
          </div>
        </>
      ) : (
        !error && <p className="muted">Gerando ticket...</p>
      )}
    </BottomSheet>
  )
}
