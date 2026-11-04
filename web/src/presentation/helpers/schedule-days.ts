import { addDays } from '@/main/config/business-time'

const weekdayFormatter = new Intl.DateTimeFormat('pt-BR', {
  weekday: 'short',
  timeZone: 'UTC',
})

const DAYS_SHOWN = 14

/** Next open days (Sundays only when the shop opens) as 'YYYY-MM-DD'. */
export function upcomingDays(
  today: string,
  openOnSunday: boolean,
  count = DAYS_SHOWN,
): string[] {
  const days: string[] = []
  for (let offset = 0; days.length < count && offset < count * 2; offset++) {
    const day = addDays(today, offset)
    const [year, month, date] = day.split('-').map(Number)
    const isSunday = new Date(Date.UTC(year, month - 1, date)).getUTCDay() === 0
    if (!isSunday || openOnSunday) days.push(day)
  }
  return days
}

export function dayLabel(day: string, today: string): string {
  if (day === today) return 'Hoje'
  if (day === addDays(today, 1)) return 'Amanhã'
  const [year, month, date] = day.split('-').map(Number)
  return weekdayFormatter
    .format(new Date(Date.UTC(year, month - 1, date)))
    .replace('.', '')
}
