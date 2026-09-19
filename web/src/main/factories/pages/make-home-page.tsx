import { Home } from '@/presentation/pages/home/home'
import { makeRemoteLoadHealthStatus } from '@/main/factories/usecases/make-remote-load-health-status'

export const makeHomePage = () => {
  return <Home loadHealthStatus={makeRemoteLoadHealthStatus()} />
}
