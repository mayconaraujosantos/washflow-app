import { makeRemoteAuthenticateCustomer } from '@/main/factories/usecases/make-remote-authenticate-customer'
import { makeRemoteAuthenticateManager } from '@/main/factories/usecases/make-remote-authenticate-manager'
import { makeRemoteAuthenticateWasher } from '@/main/factories/usecases/make-remote-authenticate-washer'
import { Home } from '@/presentation/pages/home/home'

export const makeHomePage = () => (
  <Home
    authenticateCustomer={makeRemoteAuthenticateCustomer()}
    authenticateWasher={makeRemoteAuthenticateWasher()}
    authenticateManager={makeRemoteAuthenticateManager()}
  />
)
