import { AuthenticateCustomer } from '@/domain/usecases/authenticate-customer'
import { Home } from '@/presentation/pages/home/home'

type Props = Readonly<{
  authenticateCustomer: AuthenticateCustomer
}>

/** Target of the customer QR code: the access screen, customer profile only. */
export function CustomerLogin({ authenticateCustomer }: Props) {
  return <Home authenticateCustomer={authenticateCustomer} />
}
