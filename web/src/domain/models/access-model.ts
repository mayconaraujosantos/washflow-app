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
  /** E-mail channel on: sign in with a code by e-mail and reset a forgotten password by e-mail. */
  emailCode: boolean
  googleClientId: string | null
}

/** Who asks for a password-reset code: customers by phone, managers by login e-mail. */
export type PasswordResetAccount =
  { kind: 'customer'; phone: string } | { kind: 'manager'; email: string }

// Where the one-time code goes: a phone (WhatsApp) or an e-mail address.
export type CodeRecipient = { phone: string } | { email: string }
