export type UserProfile = 'CUSTOMER' | 'WASHER' | 'MANAGER'

export type UserModel = {
  id: string
  name: string
  phone: string
  profile: UserProfile
  createdAt: string
}
