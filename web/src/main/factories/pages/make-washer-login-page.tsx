import { makeRemoteAccess } from '@/main/factories/usecases/make-remote-access'
import { makeRemoteAuthenticateWasher } from '@/main/factories/usecases/make-remote-authenticate-washer'
import { WasherLogin } from '@/presentation/pages/washer-login/washer-login'

export const makeWasherLoginPage = () => (
  <WasherLogin
    authenticateWasher={makeRemoteAuthenticateWasher()}
    access={makeRemoteAccess()}
  />
)
