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
      subtitle="Entre com o telefone e a senha cadastrados como gerente."
      passwordLabel="Senha"
      panelPath="/gerente"
      panelLabel="Abrir painel do gerente"
      authenticate={(params) => authenticateManager.auth(params)}
      saveSession={saveManagerSession}
    />
  )
}
