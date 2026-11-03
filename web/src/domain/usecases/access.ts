import type { AuthenticatedSessionModel } from '@/domain/models/authenticated-session-model'
import type {
  AuthOptionsModel,
  TicketModel,
  WasherBadgeModel,
} from '@/domain/models/access-model'

/** The code was right but the phone has no account yet: ask the customer's name. */
export class NeedsNameError extends Error {
  constructor(message = 'Informe seu nome para criar a conta') {
    super(message)
    this.name = 'NeedsNameError'
  }
}

export interface Access {
  issueWasherBadge: (washerId: string) => Promise<WasherBadgeModel>
  washerPinLogin: (
    phone: string,
    pin: string,
  ) => Promise<AuthenticatedSessionModel>
  washerBadgeLogin: (badgeToken: string) => Promise<AuthenticatedSessionModel>
  issueTicket: (serviceOrderId: string) => Promise<string>
  loadTicket: (token: string) => Promise<TicketModel>
  options: () => Promise<AuthOptionsModel>
  requestCode: (phone: string) => Promise<number>
  /** Rejects with NeedsNameError for a new phone without name. */
  verifyCode: (
    phone: string,
    code: string,
    name?: string,
  ) => Promise<AuthenticatedSessionModel>
  googleLogin: (credential: string) => Promise<AuthenticatedSessionModel>
}
