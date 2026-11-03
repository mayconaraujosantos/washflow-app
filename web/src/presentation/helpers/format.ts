import { BUSINESS_TIME_ZONE, manausHour } from '@/main/config/business-time'

const moneyFormatter = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
})

const timeFormatter = new Intl.DateTimeFormat('pt-BR', {
  timeZone: BUSINESS_TIME_ZONE,
  hour: '2-digit',
  minute: '2-digit',
})

const dateTimeFormatter = new Intl.DateTimeFormat('pt-BR', {
  timeZone: BUSINESS_TIME_ZONE,
  day: '2-digit',
  month: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
})

export function formatMoney(value: number | string | null | undefined): string {
  return moneyFormatter.format(Number(value ?? 0))
}

export function formatTime(instant: string): string {
  return timeFormatter.format(new Date(instant))
}

export function formatDateTime(instant: string): string {
  return dateTimeFormatter.format(new Date(instant))
}

/** "Bom dia" / "Boa tarde" / "Boa noite" by the shop's (Manaus) clock. */
export function greeting(now: Date = new Date()): string {
  const hour = manausHour(now)
  if (hour < 12) return 'Bom dia'
  if (hour < 18) return 'Boa tarde'
  return 'Boa noite'
}

export function firstName(name: string | null | undefined): string {
  return (name ?? '').trim().split(/\s+/)[0] ?? ''
}

/** Formats a Brazilian phone ("92991234567" -> "(92) 99123-4567") for display. */
export function formatPhone(phone: string | null | undefined): string {
  const digits = (phone ?? '').replace(/\D/g, '')
  if (digits.length === 11) {
    return `(${digits.slice(0, 2)}) ${digits.slice(2, 7)}-${digits.slice(7)}`
  }
  if (digits.length === 10) {
    return `(${digits.slice(0, 2)}) ${digits.slice(2, 6)}-${digits.slice(6)}`
  }
  return phone ?? ''
}

/** Upper-case alphanumerics only, max 7 - the shape of a Brazilian plate. */
export function normalizePlate(value: string): string {
  return value
    .toUpperCase()
    .replace(/[^A-Z0-9]/g, '')
    .slice(0, 7)
}

export function errorMessage(cause: unknown, fallback: string): string {
  return cause instanceof Error && cause.message ? cause.message : fallback
}

/**
 * Parses a typed amount ("12,50", "1.234,56" or "12.50") into a number with at
 * most 2 decimals; null when it is not a valid non-negative amount.
 */
export function parseMoney(value: string): number | null {
  const trimmed = value.trim()
  const normalized = trimmed.includes(',')
    ? trimmed.replace(/\./g, '').replace(',', '.')
    : trimmed
  if (!/^\d+(\.\d{1,2})?$/.test(normalized)) return null
  return Number(normalized)
}
