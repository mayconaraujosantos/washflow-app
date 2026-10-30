import { ServiceOrderStatus } from '@/domain/models/service-order-model'

export const STATUS_LABELS: Record<ServiceOrderStatus, string> = {
  SCHEDULED: 'Agendado',
  WAITING_IN_YARD: 'Aguardando no pátio',
  WASHING: 'Em lavagem',
  PAUSED: 'Pausada',
  READY: 'Pronto',
  DONE: 'Finalizado',
}

export const STATUS_LABELS_DETAILED: Record<ServiceOrderStatus, string> = {
  SCHEDULED: 'Agendado',
  WAITING_IN_YARD: 'Aguardando no pátio',
  WASHING: 'Em lavagem',
  PAUSED: 'Lavagem pausada',
  READY: 'Pronto para retirada',
  DONE: 'Finalizado',
}

export const PAUSE_LABELS: Record<string, string> = {
  LUNCH: 'Almoço',
  RESTROOM: 'Banheiro',
  OTHER: 'Outro',
}

export const PAUSE_LABELS_LOWER: Record<string, string> = {
  LUNCH: 'almoço',
  RESTROOM: 'banheiro',
  OTHER: 'outro motivo',
}

export const PAYMENT_LABELS: Record<string, string> = {
  CASH: 'Dinheiro',
  PIX: 'Pix',
  DEBIT_CARD: 'Cartão de débito',
  CREDIT_CARD: 'Cartão de crédito',
}
