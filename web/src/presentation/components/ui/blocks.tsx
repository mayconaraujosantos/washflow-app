import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import type { ServiceOrderModel } from '@/domain/models/service-order-model'
import { PAUSE_LABELS, STATUS_LABELS } from '@/main/config/labels'
import { UiIcon, type UiIconName } from '@/presentation/components/ui-icon'
import {
  formatDateTime,
  formatPhone,
  formatTime,
} from '@/presentation/helpers/format'
import { useEscape } from './use-escape'

export function KpiBand({
  items,
}: Readonly<{ items: { label: string; value: ReactNode }[] }>) {
  return (
    <section className="kpi-band" aria-label="Indicadores">
      {items.map((item) => (
        <div key={item.label}>
          <span>{item.label}</span>
          <strong>{item.value}</strong>
        </div>
      ))}
    </section>
  )
}

type TileProps = Readonly<{
  to: string
  label: string
  icon: UiIconName
  tone: 'entry' | 'exit' | 'schedule' | 'budget'
  hint?: string
}>

export function ActionTile({ to, label, icon, tone, hint }: TileProps) {
  return (
    <Link to={to} className={`tile tile-${tone}`}>
      <UiIcon name={icon} size={44} />
      {label}
      {hint && <small>{hint}</small>}
    </Link>
  )
}

type EmptyProps = Readonly<{
  icon: UiIconName
  title: string
  description?: ReactNode
  accent?: string
}>

export function EmptyState({ icon, title, description, accent }: EmptyProps) {
  return (
    <div className="empty">
      <UiIcon name={icon} size={56} />
      <p className="empty-title">{title}</p>
      {description && <p>{description}</p>}
      {accent && <p className="empty-accent">{accent}</p>}
    </div>
  )
}

export function Notice({
  tone,
  children,
}: Readonly<{
  tone: 'error' | 'success' | 'info' | 'warning'
  children: ReactNode
}>) {
  if (!children) return null
  return (
    <p
      className={`notice notice-${tone}`}
      role={tone === 'error' ? 'alert' : 'status'}
    >
      {children}
    </p>
  )
}

export function StatusBadge({
  status,
}: Readonly<{ status: ServiceOrderModel['status'] }>) {
  return (
    <span className={`status-badge status-${status.toLowerCase()}`}>
      {STATUS_LABELS[status]}
    </span>
  )
}

type OrderCardProps = Readonly<{
  order: ServiceOrderModel
  showCustomer?: boolean
  showWasher?: boolean
  timeLabel?: 'time' | 'datetime'
  children?: ReactNode
}>

/** One service order: plate first (what people look for in the yard). */
export function OrderCard({
  order,
  showCustomer = true,
  showWasher = false,
  timeLabel = 'time',
  children,
}: OrderCardProps) {
  const details = order.details
  const vehicle = [details?.vehicleModel, details?.vehicleColor]
    .filter(Boolean)
    .join(' · ')
  return (
    <li className="order-card" data-status={order.status}>
      <div className="order-head">
        <div>
          <span className="plate">{details?.vehiclePlate ?? '—'}</span>
          {vehicle && <p className="muted">{vehicle}</p>}
        </div>
        <StatusBadge status={order.status} />
      </div>
      <div className="order-meta">
        {details?.serviceName && (
          <span>
            <UiIcon name="tag" size={15} />
            {details.serviceName}
          </span>
        )}
        <span>
          <UiIcon name="clock" size={15} />
          {timeLabel === 'datetime'
            ? formatDateTime(order.scheduledAt)
            : formatTime(order.scheduledAt)}
        </span>
        {order.expectedDeliveryAt && (
          <span>
            <UiIcon name="calendar" size={15} />
            Entrega {formatDateTime(order.expectedDeliveryAt)}
          </span>
        )}
        {showCustomer && details?.customerName && (
          <span>
            <UiIcon name="user" size={15} />
            {details.customerName}
          </span>
        )}
        {showCustomer && details?.customerPhone && (
          <span>
            <UiIcon name="phone" size={15} />
            {formatPhone(details.customerPhone)}
          </span>
        )}
        {showWasher && details?.washerName && (
          <span>
            <UiIcon name="washer" size={15} />
            {details.washerName}
          </span>
        )}
      </div>
      {order.status === 'PAUSED' && order.pauseReason && (
        <p className="faint">Pausada: {PAUSE_LABELS[order.pauseReason]}</p>
      )}
      {order.notes && <p className="faint">“{order.notes}”</p>}
      {children && <div className="order-actions">{children}</div>}
    </li>
  )
}

type SheetProps = Readonly<{
  open: boolean
  title: string
  onClose: () => void
  children: ReactNode
}>

/** Modal panel sliding up from the bottom, closed by Escape or backdrop. */
export function BottomSheet({ open, title, onClose, children }: SheetProps) {
  useEscape(open, onClose)
  if (!open) return null
  return (
    <div
      className="sheet-backdrop"
      onClick={(event) => {
        if (event.target === event.currentTarget) onClose()
      }}
    >
      <section
        className="sheet"
        role="dialog"
        aria-modal="true"
        aria-label={title}
      >
        <span className="sheet-grip" aria-hidden="true" />
        <div className="sheet-head">
          <h2>{title}</h2>
          <button
            type="button"
            className="icon-btn"
            aria-label="Fechar"
            onClick={onClose}
          >
            <UiIcon name="close" />
          </button>
        </div>
        {children}
      </section>
    </div>
  )
}
