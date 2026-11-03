import { AuthenticateCustomer } from '@/domain/usecases/authenticate-customer'
import { AccessScreen } from '@/presentation/pages/access/access-screen'

type Props = Readonly<{
  authenticateCustomer: AuthenticateCustomer
}>

/** Target of "Cliente > Entrar" and of the customer QR code. */
export function CustomerLogin({ authenticateCustomer }: Props) {
  return <AccessScreen authenticateCustomer={authenticateCustomer} />
}
