import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import type { ServiceOrderModel } from '@/domain/models/service-order-model'
import { addDays, manausDate } from '@/main/config/business-time'
import { UiIcon } from '@/presentation/components/ui-icon'
import { ScreenLayout } from '@/presentation/components/ui/app-chrome'
import {
  EmptyState,
  Notice,
  OrderCard,
} from '@/presentation/components/ui/blocks'
import { errorMessage, formatMoney } from '@/presentation/helpers/format'
import { MANAGER_NAV, useManager } from './manager-context'
import { RefundSheet } from './refund-sheet'

const WEEKDAYS = ['dom.', 'seg.', 'ter.', 'qua.', 'qui.', 'sex.', 'sáb.']

const monthFormatter = new Intl.DateTimeFormat('pt-BR', {
  month: 'long',
  year: 'numeric',
  timeZone: 'UTC',
})

function monthBounds(month: string) {
  const first = `${month}-01`
  const [year, monthNumber] = month.split('-').map(Number)
  const nextMonth = new Date(Date.UTC(year, monthNumber, 1))
    .toISOString()
    .slice(0, 10)
  return { first, last: addDays(nextMonth, -1), year, monthNumber }
}

function shiftMonth(month: string, delta: number) {
  const [year, monthNumber] = month.split('-').map(Number)
  return new Date(Date.UTC(year, monthNumber - 1 + delta, 1))
    .toISOString()
    .slice(0, 7)
}

/** Daily capacity: open hours x vehicles per hour (0 on a closed Sunday). */
function dayCapacity(
  day: string,
  settings: {
    openingHour: number
    closingHour: number
    openOnSunday: boolean
    maxVehiclesPerHour: number
  } | null,
): number {
  if (!settings) return 0
  const [year, month, date] = day.split('-').map(Number)
  const sunday = new Date(Date.UTC(year, month - 1, date)).getUTCDay() === 0
  if (sunday && !settings.openOnSunday) return 0
  return (
    (settings.closingHour - settings.openingHour) * settings.maxVehiclesPerHour
  )
}

export function AgendaScreen() {
  const { operations, openMenu, settings, notify } = useManager()
  const today = manausDate()
  const [month, setMonth] = useState(today.slice(0, 7))
  const [selected, setSelected] = useState(today)
  const [orders, setOrders] = useState<ServiceOrderModel[]>([])
  const [error, setError] = useState<string | null>(null)
  const [refunding, setRefunding] = useState<ServiceOrderModel | null>(null)
  const [busyId, setBusyId] = useState<string | null>(null)

  const { first, last, year, monthNumber } = monthBounds(month)

  useEffect(() => {
    let active = true
    operations
      .loadAgenda(first, last)
      .then((result) => {
        if (!active) return
        setOrders(result)
        setError(null)
      })
      .catch((cause) => {
        if (active) setError(errorMessage(cause, 'Falha ao carregar a agenda'))
      })
    return () => {
      active = false
    }
  }, [operations, first, last])

  const byDay = useMemo(() => {
    const map = new Map<string, ServiceOrderModel[]>()
    for (const order of orders) {
      const day = manausDate(order.scheduledAt)
      map.set(day, [...(map.get(day) ?? []), order])
    }
    return map
  }, [orders])

  const leadingBlanks = new Date(Date.UTC(year, monthNumber - 1, 1)).getUTCDay()
  const daysInMonth = Number(last.slice(8, 10))
  const days = Array.from({ length: daysInMonth }, (_, index) =>
    addDays(first, index),
  )
  const selectedOrders = byDay.get(selected) ?? []
  const selectedLabel = `${selected.slice(8, 10)}/${selected.slice(5, 7)}`

  return (
    <ScreenLayout onMenu={openMenu} nav={MANAGER_NAV}>
      <div className="calendar-head">
        <button
          type="button"
          className="icon-btn"
          aria-label="Mês anterior"
          onClick={() => setMonth(shiftMonth(month, -1))}
        >
          <UiIcon name="chevronLeft" />
        </button>
        <h2>{monthFormatter.format(Date.UTC(year, monthNumber - 1, 1))}</h2>
        <button
          type="button"
          className="icon-btn"
          aria-label="Próximo mês"
          onClick={() => setMonth(shiftMonth(month, 1))}
        >
          <UiIcon name="chevronRight" />
        </button>
      </div>

      <Notice tone="error">{error}</Notice>

      <div className="calendar-grid" aria-label="Calendário">
        {WEEKDAYS.map((weekday) => (
          <span key={weekday} className="calendar-weekday">
            {weekday}
          </span>
        ))}
        {Array.from({ length: leadingBlanks }, (_, index) => (
          <span key={`blank-${index}`} />
        ))}
        {days.map((day) => {
          // Cancelled orders stay listed for the day but no longer take a slot.
          const count =
            byDay.get(day)?.filter((order) => order.status !== 'CANCELLED')
              .length ?? 0
          const max = dayCapacity(day, settings)
          return (
            <button
              key={day}
              type="button"
              className={`calendar-day${day === today ? ' is-today' : ''}`}
              aria-pressed={day === selected}
              aria-label={`Dia ${Number(day.slice(8, 10))}: ${count} agendado(s) de ${max}`}
              onClick={() => setSelected(day)}
            >
              <span className="calendar-num">{Number(day.slice(8, 10))}</span>
              <span className="calendar-max">Máx: {max}</span>
              <span className={`calendar-count${count ? '' : ' is-zero'}`}>
                Ag: {count}
              </span>
            </button>
          )
        })}
      </div>

      <section className="form">
        <div className="section-header">
          <h2>Dia {selectedLabel}</h2>
          <Link
            to={`/gerente/agendar?data=${selected}`}
            className="btn btn-accent btn-small"
          >
            <UiIcon name="plus" size={16} />
            Agendar
          </Link>
        </div>
        {selectedOrders.length === 0 ? (
          <EmptyState icon="calendar" title="Nenhum agendamento neste dia." />
        ) : (
          <ul className="list">
            {selectedOrders.map((order) => (
              <OrderCard key={order.id} order={order} showWasher>
                {order.status === 'DONE' && (
                  <button
                    type="button"
                    className="btn btn-ghost"
                    disabled={busyId === order.id}
                    onClick={() => setRefunding(order)}
                  >
                    <UiIcon name="money" size={18} />
                    Estornar
                  </button>
                )}
              </OrderCard>
            ))}
          </ul>
        )}
      </section>
      <RefundSheet
        order={refunding}
        amount={refunding?.details?.serviceTotal ?? undefined}
        onClose={() => setRefunding(null)}
        onConfirm={(order, params) => {
          setRefunding(null)
          setBusyId(order.id)
          operations
            .refund(order.id, params)
            .then((refund) => {
              setOrders((current) =>
                current.map((item) =>
                  item.id === order.id ? { ...item, status: 'REFUNDED' } : item,
                ),
              )
              notify(
                'success',
                `Estorno de ${formatMoney(refund.amount)} registrado.`,
              )
            })
            .catch((cause) =>
              notify('error', errorMessage(cause, 'Falha ao estornar')),
            )
            .finally(() => setBusyId(null))
        }}
      />
    </ScreenLayout>
  )
}
