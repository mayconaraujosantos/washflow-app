import { AuthenticatedSessionModel } from '@/domain/models/authenticated-session-model'

export interface AuthenticateManager {
  auth: (
    params: AuthenticateManager.Params,
  ) => Promise<AuthenticateManager.Model>
}

export namespace AuthenticateManager {
  // Managers log in with e-mail (preferred) or, for older accounts, phone.
  export type Params =
    { email: string; password: string } | { phone: string; password: string }

  export type Model = AuthenticatedSessionModel
}
