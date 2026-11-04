import type { Access } from '@/domain/usecases/access'
import { AuthenticateCustomer } from '@/domain/usecases/authenticate-customer'
import type { EstablishmentDirectory } from '@/domain/usecases/platform'
import { AccessScreen } from '@/presentation/pages/access/access-screen'

type Props = Readonly<{
  authenticateCustomer: AuthenticateCustomer
  access?: Access
  directory?: EstablishmentDirectory
}>

/** Target of "Cliente > Entrar" and of the customer QR code. */
export function CustomerLogin({
  authenticateCustomer,
  access,
  directory,
}: Props) {
  return (
    <AccessScreen
      authenticateCustomer={authenticateCustomer}
      access={access}
      directory={directory}
    />
  )
}
