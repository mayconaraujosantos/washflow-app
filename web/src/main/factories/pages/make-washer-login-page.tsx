import { WasherLogin } from '@/presentation/pages/washer-login/washer-login'
import { makeRemoteAuthenticateWasher } from '@/main/factories/usecases/make-remote-authenticate-washer'

export const makeWasherLoginPage = () => {
  return <WasherLogin authenticateWasher={makeRemoteAuthenticateWasher()} />
}
