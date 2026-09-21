import { UserModel } from '@/domain/models/user-model'

export interface AuthenticateWasher {
  auth: (
    params: AuthenticateWasher.Params,
  ) => Promise<AuthenticateWasher.Model>
}

export namespace AuthenticateWasher {
  export type Params = {
    phone: string
    name: string
  }

  export type Model = UserModel
}
