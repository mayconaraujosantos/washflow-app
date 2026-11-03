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
import { errorMessage } from '@/presentation/helpers/format'
import { MANAGER_NAV, useManager } from './manager-context'

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

export function AgendaScreen() {
  const { operations, openMenu } = useManager()
  const today = manausDate()
  const [month, setMonth] = useState(today.slice(0, 7))
  const [selected, setSelected] = useState(today)
  const [orders, setOrders] = useState<ServiceOrderModel[]>([])
  const [error, setError] = useState<string | null>(null)

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

      <div className="calendar-grid" role="grid" aria-label="Calendário">
        {WEEKDAYS.map((weekday) => (
          <span key={weekday} className="calendar-weekday">
            {weekday}
          </span>
        ))}
        {Array.from({ length: leadingBlanks }, (_, index) => (
          <span key={`blank-${index}`} />
        ))}
        {days.map((day) => {
          const count = byDay.get(day)?.length ?? 0
          return (
            <button
              key={day}
              type="button"
              className={`calendar-day${day === today ? ' is-today' : ''}`}
              aria-pressed={day === selected}
              aria-label={`${Number(day.slice(8, 10))}: ${count} agendamento(s)`}
              onClick={() => setSelected(day)}
            >
              <span className="calendar-num">{Number(day.slice(8, 10))}</span>
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
              <OrderCard key={order.id} order={order} showWasher />
            ))}
          </ul>
        )}
      </section>
    </ScreenLayout>
  )
}
