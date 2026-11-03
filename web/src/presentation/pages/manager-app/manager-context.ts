import { createContext, useContext } from 'react'
import type { ServiceOrderModel } from '@/domain/models/service-order-model'
import type { ServicePriceModel } from '@/domain/models/service-price-model'
import type { UserModel } from '@/domain/models/user-model'
import type { Features } from '@/domain/usecases/features'
import type { Operations, PaymentMethod } from '@/domain/usecases/operations'
import type { NavItem } from '@/presentation/components/ui/app-chrome'

export type FlashTone = 'success' | 'error' | 'info'

export type ManagerData = {
  user: UserModel
  operations: Operations
  features: Features
  yard: ServiceOrderModel[]
  servicePrices: ServicePriceModel[]
  refresh: () => Promise<void>
  reloadPrices: () => Promise<void>
  notify: (tone: FlashTone, text: string) => void
  openMenu: () => void
}

export const MANAGER_NAV: NavItem[] = [
  { to: '/gerente', label: 'Início', icon: 'home', end: true },
  { to: '/gerente/patio', label: 'Pátio', icon: 'parking' },
  { to: '/gerente/agenda', label: 'Agenda', icon: 'calendar' },
]

export const ManagerContext = createContext<ManagerData | null>(null)

export function useManager(): ManagerData {
  const data = useContext(ManagerContext)
  if (!data) throw new Error('useManager must be used inside ManagerApp')
  return data
}

/** Orders physically in the shop (arrived and not yet picked up). */
export const IN_YARD_STATUSES: ServiceOrderModel['status'][] = [
  'WAITING_IN_YARD',
  'WASHING',
  'PAUSED',
  'READY',
]

export const PAYMENT_METHODS: PaymentMethod[] = [
  'PIX',
  'CASH',
  'DEBIT_CARD',
  'CREDIT_CARD',
]
