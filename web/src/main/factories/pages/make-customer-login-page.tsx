import { makeRemoteAccess } from '@/main/factories/usecases/make-remote-access'
import { makeRemoteAuthenticateCustomer } from '@/main/factories/usecases/make-remote-authenticate-customer'
import { makeRemotePlatform } from '@/main/factories/usecases/make-remote-platform'
import { CustomerLogin } from '@/presentation/pages/customer-login/customer-login'

export const makeCustomerLoginPage = () => (
  <CustomerLogin
    authenticateCustomer={makeRemoteAuthenticateCustomer()}
    access={makeRemoteAccess()}
    directory={makeRemotePlatform()}
  />
)
