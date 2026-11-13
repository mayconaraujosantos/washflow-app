import type { WasherStatusModel } from '@/domain/models/washer-status-model'
import { PAUSE_LABELS_LOWER } from '@/main/config/labels'

/** Whole minutes since `instant` ("agora" under one minute). */
export function minutesSince(instant: string, now: Date = new Date()): string {
  const minutes = Math.floor(
    (now.getTime() - new Date(instant).getTime()) / 60_000,
  )
  if (minutes < 1) return 'agora'
  if (minutes < 60) return `há ${minutes} min`
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  return rest === 0 ? `há ${hours} h` : `há ${hours} h ${rest} min`
}

/** True when `instant` is more than `ms` milliseconds ago. */
export function olderThan(
  instant: string,
  ms: number,
  now: Date = new Date(),
): boolean {
  return now.getTime() - new Date(instant).getTime() > ms
}

/** "Livre", "Lavando ABC1D23 há 12 min" or "Em pausa (almoço) há 5 min". */
export function washerStateText(
  washer: WasherStatusModel,
  now: Date = new Date(),
): string {
  const current = washer.current
  if (washer.state === 'FREE' || !current) return 'Livre'
  if (washer.state === 'PAUSED') {
    const reason = current.pauseReason
      ? ` (${PAUSE_LABELS_LOWER[current.pauseReason] ?? current.pauseReason})`
      : ''
    const since = current.pausedAt
      ? ` ${minutesSince(current.pausedAt, now)}`
      : ''
    return `Em pausa${reason}${since} · ${current.vehiclePlate}`
  }
  const since = current.since ? ` ${minutesSince(current.since, now)}` : ''
  return `Lavando ${current.vehiclePlate}${since}`
}

/** "1 carro reservado" / "2 carros reservados"; empty when none. */
export function reservedText(washer: WasherStatusModel): string {
  if (washer.reserved === 0) return ''
  return washer.reserved === 1
    ? '1 carro reservado'
    : `${washer.reserved} carros reservados`
}
