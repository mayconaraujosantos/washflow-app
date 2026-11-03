import { AuthenticateWasher } from '@/domain/usecases/authenticate-washer'
import { saveWasherSession } from '@/main/session/washer-session'
import { StaffLogin } from '@/presentation/components/staff-login'

type Props = Readonly<{
  authenticateWasher: AuthenticateWasher
}>

export function WasherLogin({ authenticateWasher }: Props) {
  return (
    <StaffLogin
      title="Entrar como lavador"
      subtitle="Entre com o telefone e a senha fornecidos pelo gerente."
      passwordLabel="Senha (mínimo 8 caracteres)"
      panelPath="/lavador"
      panelLabel="Abrir painel do lavador"
      authenticate={(params) => authenticateWasher.auth(params)}
      saveSession={saveWasherSession}
    />
  )
}
