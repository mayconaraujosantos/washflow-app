import { makeRemotePlatform } from '@/main/factories/usecases/make-remote-platform'
import { AdminLogin } from '@/presentation/pages/admin/admin-login'
import { AdminPanel } from '@/presentation/pages/admin/admin-panel'

export const makeAdminLoginPage = () => (
  <AdminLogin platform={makeRemotePlatform()} />
)

export const makeAdminPanelPage = () => (
  <AdminPanel platform={makeRemotePlatform()} />
)
