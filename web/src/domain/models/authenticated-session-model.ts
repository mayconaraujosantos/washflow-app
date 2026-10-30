import { UserModel } from './user-model'

export type AuthenticatedSessionModel = {
  user: UserModel
  token: string
}
