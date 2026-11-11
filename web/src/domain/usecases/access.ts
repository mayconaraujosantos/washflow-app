import type { AuthenticatedSessionModel } from '@/domain/models/authenticated-session-model'
import type {
  AuthOptionsModel,
  PasswordResetAccount,
  CodeRecipient,
  TicketModel,
  WasherBadgeModel,
} from '@/domain/models/access-model'

/** The code was right but the phone/e-mail has no account yet: ask the customer's name. */
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
  requestCode: (to: CodeRecipient) => Promise<number>
  /** Rejects with NeedsNameError for a new phone/e-mail without name. */
  verifyCode: (
    to: CodeRecipient,
    code: string,
    name?: string,
  ) => Promise<AuthenticatedSessionModel>
  googleLogin: (credential: string) => Promise<AuthenticatedSessionModel>
  /** E-mails a 6-digit code; answers the same whether the account exists. */
  requestPasswordReset: (account: PasswordResetAccount) => Promise<number>
  confirmPasswordReset: (
    account: PasswordResetAccount,
    code: string,
    password: string,
  ) => Promise<void>
  /** "Meus dados": where the customer's reset codes go. */
  loadRecoveryEmail: (customerId: string) => Promise<string | null>
  /** `accessToken` right after sign-up, before the page knows the session. */
  saveRecoveryEmail: (
    customerId: string,
    email: string | null,
    accessToken?: string,
  ) => Promise<string | null>
}
