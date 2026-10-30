import { ManagerLogin } from '@/presentation/pages/manager-login/manager-login'
import { makeRemoteAuthenticateManager } from '@/main/factories/usecases/make-remote-authenticate-manager'

export const makeManagerLoginPage = () => {
  return <ManagerLogin authenticateManager={makeRemoteAuthenticateManager()} />
}
