import { AuthenticatedSessionModel } from '@/domain/models/authenticated-session-model'

export interface AuthenticateWasher {
  auth: (params: AuthenticateWasher.Params) => Promise<AuthenticateWasher.Model>
}

export namespace AuthenticateWasher {
  export type Params = {
    phone: string
    password: string
  }

  export type Model = AuthenticatedSessionModel
}
