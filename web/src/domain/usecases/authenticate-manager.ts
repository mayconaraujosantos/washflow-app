import { AuthenticatedSessionModel } from '@/domain/models/authenticated-session-model'

export interface AuthenticateManager {
  auth: (
    params: AuthenticateManager.Params,
  ) => Promise<AuthenticateManager.Model>
}

export namespace AuthenticateManager {
  export type Params = {
    phone: string
    password: string
  }

  export type Model = AuthenticatedSessionModel
}
