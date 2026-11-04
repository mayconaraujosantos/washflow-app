import type { ServiceOrderStatus } from '@/domain/models/service-order-model'

// POST /api/users/{id}/badge - shown once; the API keeps only hashes.
export type WasherBadgeModel = { pin: string; badgeToken: string }

// GET /api/tickets/{token} - public view of one order (no customer data).
export type TicketModel = {
  establishmentName: string
  vehiclePlate: string
  vehicleModel: string | null
  serviceName: string
  status: ServiceOrderStatus
  pauseReason: 'LUNCH' | 'RESTROOM' | 'OTHER' | null
  scheduledAt: string
  expectedDeliveryAt: string | null
  updatedAt: string
}

// GET /api/auth/options - which passwordless customer logins are enabled.
export type AuthOptionsModel = {
  whatsappCode: boolean
  googleClientId: string | null
}
