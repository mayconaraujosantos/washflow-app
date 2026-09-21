import { CustomerLogin } from '@/presentation/pages/customer-login/customer-login'
import { makeRemoteAuthenticateCustomer } from '@/main/factories/usecases/make-remote-authenticate-customer'

export const makeCustomerLoginPage = () => {
  return (
    <CustomerLogin authenticateCustomer={makeRemoteAuthenticateCustomer()} />
  )
}
