import { makeRemoteEstablishment } from '@/main/factories/usecases/make-remote-establishment'
import { makeRemoteLoadHealthStatus } from '@/main/factories/usecases/make-remote-load-health-status'
import { Home } from '@/presentation/pages/home/home'

export const makeHomePage = () => (
  <Home
    loadHealthStatus={makeRemoteLoadHealthStatus()}
    establishment={makeRemoteEstablishment()}
  />
)
