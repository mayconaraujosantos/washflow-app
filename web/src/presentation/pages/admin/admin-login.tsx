import type { Platform } from '@/domain/usecases/platform'
import { saveAdminSession } from '@/main/session/admin-session'
import { StaffLogin } from '@/presentation/components/staff-login'

type Props = Readonly<{ platform: Platform }>

/** Platform administrator sign-in (e-mail + password). */
export function AdminLogin({ platform }: Props) {
  return (
    <StaffLogin
      title="Administração"
      subtitle="Acesso do administrador da plataforma."
      passwordLabel="Senha"
      panelPath="/admin"
      panelLabel="Abrir a administração"
      identifier="emailOrPhone"
      authenticate={(credentials) =>
        platform.signIn(
          'email' in credentials ? credentials.email : credentials.phone,
          credentials.password,
        )
      }
      saveSession={saveAdminSession}
    />
  )
}
