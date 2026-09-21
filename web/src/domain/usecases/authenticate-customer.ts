import { UserModel } from '@/domain/models/user-model'

export interface AuthenticateCustomer {
  auth: (
    params: AuthenticateCustomer.Params,
  ) => Promise<AuthenticateCustomer.Model>
}

export namespace AuthenticateCustomer {
  export type Params = {
    phone: string
    name: string
  }

  export type Model = UserModel
}
