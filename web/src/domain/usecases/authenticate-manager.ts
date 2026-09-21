import { UserModel } from '@/domain/models/user-model'

export interface AuthenticateManager {
  auth: (
    params: AuthenticateManager.Params,
  ) => Promise<AuthenticateManager.Model>
}

export namespace AuthenticateManager {
  export type Params = {
    phone: string
    name: string
  }

  export type Model = UserModel
}
