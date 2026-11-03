export const BUSINESS_TIME_ZONE = 'America/Manaus'

const manausDateTimeFormatter = new Intl.DateTimeFormat('en-CA', {
  timeZone: BUSINESS_TIME_ZONE,
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit',
  hourCycle: 'h23',
})

export function toManausInstant(localDateTime: string): string {
  const match = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})$/.exec(localDateTime)
  if (!match) throw new RangeError('Selecione uma data e horário válidos.')

  const [, yearText, monthText, dayText, hourText, minuteText] = match
  const [year, month, day, hour, minute] = [
    yearText,
    monthText,
    dayText,
    hourText,
    minuteText,
  ].map(Number)
  const localAsUtc = Date.UTC(year, month - 1, day, hour, minute)
  const normalizedDate = new Date(localAsUtc)
  if (
    year < 1000 ||
    normalizedDate.getUTCFullYear() !== year ||
    normalizedDate.getUTCMonth() !== month - 1 ||
    normalizedDate.getUTCDate() !== day ||
    normalizedDate.getUTCHours() !== hour ||
    normalizedDate.getUTCMinutes() !== minute
  ) {
    throw new RangeError('Selecione uma data e horário válidos.')
  }

  let instant = localAsUtc
  for (let attempt = 0; attempt < 4; attempt += 1) {
    const parts = Object.fromEntries(
      manausDateTimeFormatter
        .formatToParts(new Date(instant))
        .map(({ type, value }) => [type, Number(value)]),
    )
    const representedAsUtc = Date.UTC(
      parts.year,
      parts.month - 1,
      parts.day,
      parts.hour,
      parts.minute,
      parts.second,
    )
    const adjustedInstant = localAsUtc - (representedAsUtc - instant)
    if (adjustedInstant === instant) break
    instant = adjustedInstant
  }

  const result = new Date(instant)
  const verifiedParts = Object.fromEntries(
    manausDateTimeFormatter
      .formatToParts(result)
      .map(({ type, value }) => [type, Number(value)]),
  )
  if (
    verifiedParts.year !== year ||
    verifiedParts.month !== month ||
    verifiedParts.day !== day ||
    verifiedParts.hour !== hour ||
    verifiedParts.minute !== minute
  ) {
    throw new RangeError('Esse horário local não existe no fuso de Manaus.')
  }
  return result.toISOString()
}

const manausDateFormatter = new Intl.DateTimeFormat('en-CA', {
  timeZone: BUSINESS_TIME_ZONE,
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
})

const manausHourFormatter = new Intl.DateTimeFormat('en-US', {
  timeZone: BUSINESS_TIME_ZONE,
  hour: 'numeric',
  hourCycle: 'h23',
})

/** 'YYYY-MM-DD' of the given instant in Manaus (the shop's civil date). */
export function manausDate(instant: Date | string = new Date()): string {
  return manausDateFormatter.format(new Date(instant))
}

/** Hour 0-23 of the given instant in Manaus. */
export function manausHour(instant: Date | string = new Date()): number {
  return Number(manausHourFormatter.format(new Date(instant)))
}

/** 'YYYY-MM-DDTHH:mm' in Manaus, the value format of <input type="datetime-local">. */
export function toManausLocalInput(instant: Date | string): string {
  const parts = Object.fromEntries(
    manausDateTimeFormatter
      .formatToParts(new Date(instant))
      .map(({ type, value }) => [type, value]),
  )
  return `${parts.year}-${parts.month}-${parts.day}T${parts.hour}:${parts.minute}`
}

/** Adds whole days to a 'YYYY-MM-DD' civil date. */
export function addDays(date: string, days: number): string {
  const [year, month, day] = date.split('-').map(Number)
  return new Date(Date.UTC(year, month - 1, day + days))
    .toISOString()
    .slice(0, 10)
}
