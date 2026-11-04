export type UserProfile = 'CUSTOMER' | 'WASHER' | 'MANAGER'

export type UserModel = {
  id: string
  name: string
  phone: string
  profile: UserProfile
  createdAt: string
  active: boolean
  // Login e-mail (managers); absent on accounts that only use the phone.
  email?: string | null
}
