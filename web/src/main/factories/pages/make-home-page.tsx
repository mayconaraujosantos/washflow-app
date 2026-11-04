import { makeRemoteAccess } from '@/main/factories/usecases/make-remote-access'
import { makeRemoteAuthenticateCustomer } from '@/main/factories/usecases/make-remote-authenticate-customer'
import { makeRemoteEstablishment } from '@/main/factories/usecases/make-remote-establishment'
import { makeRemotePlatform } from '@/main/factories/usecases/make-remote-platform'
import { Home } from '@/presentation/pages/home/home'

export const makeHomePage = () => (
  <Home
    authenticateCustomer={makeRemoteAuthenticateCustomer()}
    access={makeRemoteAccess()}
    establishment={makeRemoteEstablishment()}
    directory={makeRemotePlatform()}
  />
)
