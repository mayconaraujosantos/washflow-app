import type { Access } from '@/domain/usecases/access'
import { AuthenticateCustomer } from '@/domain/usecases/authenticate-customer'
import { AccessScreen } from '@/presentation/pages/access/access-screen'

type Props = Readonly<{
  authenticateCustomer: AuthenticateCustomer
  access?: Access
}>

/** Target of "Cliente > Entrar" and of the customer QR code. */
export function CustomerLogin({ authenticateCustomer, access }: Props) {
  return (
    <AccessScreen authenticateCustomer={authenticateCustomer} access={access} />
  )
}
