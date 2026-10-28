import { AuthenticatedSessionModel } from '@/domain/models/authenticated-session-model'

export interface AuthenticateCustomer {
  auth: (
    params: AuthenticateCustomer.Params,
  ) => Promise<AuthenticateCustomer.Model>
}

export namespace AuthenticateCustomer {
  export type Params = {
    phone: string
    name: string
    password: string
  }

  export type Model = AuthenticatedSessionModel
}
