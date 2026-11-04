import { AuthenticateManager } from '@/domain/usecases/authenticate-manager'
import { saveManagerSession } from '@/main/session/manager-session'
import { StaffLogin } from '@/presentation/components/staff-login'

type Props = Readonly<{
  authenticateManager: AuthenticateManager
}>

export function ManagerLogin({ authenticateManager }: Props) {
  return (
    <StaffLogin
      title="Entrar como gerente"
      subtitle="Use o e-mail (ou o telefone) e a senha da sua conta de gestão."
      passwordLabel="Senha"
      panelPath="/gerente"
      panelLabel="Abrir painel do gerente"
      identifier="emailOrPhone"
      authenticate={(credentials) => authenticateManager.auth(credentials)}
      saveSession={saveManagerSession}
    />
  )
}
