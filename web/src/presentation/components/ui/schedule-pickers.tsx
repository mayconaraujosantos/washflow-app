import { useEffect, useMemo, useState } from 'react'
import type { ScheduleDayModel } from '@/domain/models/establishment-model'
import type { ServicePriceModel } from '@/domain/models/service-price-model'
import { manausDate } from '@/main/config/business-time'
import { Notice } from '@/presentation/components/ui/blocks'
import { errorMessage, formatMoney } from '@/presentation/helpers/format'
import { dayLabel, upcomingDays } from '@/presentation/helpers/schedule-days'

/** Select one or more services as tappable price cards (no dropdown). */
export function ServicePicker({
  services,
  values,
  onChange,
}: Readonly<{
  services: ServicePriceModel[]
  values: number[]
  onChange: (id: number) => void
}>) {
  return (
    <div className="choice-grid" role="group" aria-label="Serviços">
      {services.map((service) => (
        <button
          key={service.id}
          type="button"
          role="checkbox"
          aria-checked={values.includes(service.id)}
          className="choice"
          onClick={() => onChange(service.id)}
        >
          <span>{service.name}</span>
          <strong>{formatMoney(service.price)}</strong>
        </button>
      ))}
    </div>
  )
}

type SlotPickerProps = Readonly<{
  loadDay: (date: string) => Promise<ScheduleDayModel>
  openOnSunday: boolean
  initialDate?: string | null
  value: string | null
  onChange: (startsAt: string | null) => void
  // Customers must book ahead (30 min on the API); staff can book any free hour.
  leadMinutes?: number
}>

/**
 * Day chips + hour buttons with remaining spots, from the same rules the API
 * enforces - full, closed or past hours cannot be picked.
 */
export function SlotPicker({
  loadDay,
  openOnSunday,
  initialDate,
  value,
  onChange,
  leadMinutes = 0,
}: SlotPickerProps) {
  const today = manausDate()
  const days = useMemo(
    () => upcomingDays(today, openOnSunday),
    [today, openOnSunday],
  )
  const [selectedDay, setSelectedDay] = useState(
    initialDate && initialDate >= today ? initialDate : days[0],
  )
  const [day, setDay] = useState<ScheduleDayModel | null>(null)
  // When the day was loaded: the lead time counts from there (render stays pure).
  const [loadedAt, setLoadedAt] = useState(0)
  // Without a preset date, skip forward from days with no free hour (e.g. late
  // in the evening) - at most once per day shown, then the user decides.
  const [autoAdvance, setAutoAdvance] = useState(!initialDate)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    loadDay(selectedDay)
      .then((loaded) => {
        if (!active) return
        const now = Date.now()
        const bookable = loaded.slots.some(
          (slot) =>
            slot.available &&
            new Date(slot.startsAt).getTime() >= now + leadMinutes * 60_000,
        )
        const next = days[days.indexOf(selectedDay) + 1]
        if (autoAdvance && !bookable && next) {
          setSelectedDay(next)
          return
        }
        setAutoAdvance(false)
        setDay(loaded)
        setLoadedAt(now)
        setError(null)
      })
      .catch((cause) => {
        if (active)
          setError(errorMessage(cause, 'Não foi possível ver os horários'))
      })
    return () => {
      active = false
    }
  }, [loadDay, selectedDay, autoAdvance, days, leadMinutes])

  const earliest = loadedAt + leadMinutes * 60_000
  const slots =
    day?.date === selectedDay
      ? day.slots.map((slot) => ({
          ...slot,
          available:
            slot.available && new Date(slot.startsAt).getTime() >= earliest,
        }))
      : null
  const free = slots?.filter((slot) => slot.available).length ?? 0

  return (
    <div className="form">
      <div className="day-strip" role="radiogroup" aria-label="Dia">
        {days.map((date) => (
          <button
            key={date}
            type="button"
            role="radio"
            aria-checked={date === selectedDay}
            className="day-chip"
            onClick={() => {
              setAutoAdvance(false)
              setSelectedDay(date)
              onChange(null)
            }}
          >
            <span>{dayLabel(date, today)}</span>
            <strong>
              {date.slice(8, 10)}/{date.slice(5, 7)}
            </strong>
          </button>
        ))}
      </div>
      <Notice tone="error">{error}</Notice>
      {!slots && !error && <p className="faint">Carregando horários...</p>}
      {slots && day && !day.open && (
        <p className="faint">Fechado neste dia. Escolha outra data.</p>
      )}
      {slots && day?.open && free === 0 && (
        <p className="faint">Sem horários livres neste dia.</p>
      )}
      {slots && slots.length > 0 && (
        <div className="slot-grid" role="radiogroup" aria-label="Horário">
          {slots.map((slot) => {
            const spots = slot.capacity - slot.booked
            const status = slot.available
              ? `${spots} ${spots === 1 ? 'vaga' : 'vagas'}`
              : spots <= 0
                ? 'Lotado'
                : 'Encerrado'
            return (
              <button
                key={slot.startsAt}
                type="button"
                role="radio"
                aria-checked={value === slot.startsAt}
                aria-label={`${slot.hour}h, ${status}`}
                className="slot"
                disabled={!slot.available}
                onClick={() => onChange(slot.startsAt)}
              >
                <strong>{String(slot.hour).padStart(2, '0')}h</strong>
                <span>{status}</span>
              </button>
            )
          })}
        </div>
      )}
    </div>
  )
}
