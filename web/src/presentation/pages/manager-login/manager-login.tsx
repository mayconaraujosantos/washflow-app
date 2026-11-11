import { useEffect, useState } from 'react'
import type { Access } from '@/domain/usecases/access'
import { AuthenticateManager } from '@/domain/usecases/authenticate-manager'
import { saveManagerSession } from '@/main/session/manager-session'
import { EmailPasswordReset } from '@/presentation/components/email-password-reset'
import { StaffLogin } from '@/presentation/components/staff-login'
import { AuthLayout } from '@/presentation/components/ui/app-chrome'

type Props = Readonly<{
  authenticateManager: AuthenticateManager
  // E-mail code reset of a forgotten password, offered when the API enables it.
  access?: Access
}>

export function ManagerLogin({ authenticateManager, access }: Props) {
  const [emailReset, setEmailReset] = useState(false)
  const [forgot, setForgot] = useState(false)

  useEffect(() => {
    if (!access) return
    let active = true
    access
      .options()
      .then((options) => {
        if (active) setEmailReset(Boolean(options.emailCode))
      })
      .catch(() => {
        // Without options the counter (platform administrator) resets it.
      })
    return () => {
      active = false
    }
  }, [access])

  if (forgot && access) {
    return (
      <AuthLayout
        title="Esqueci a senha"
        footer={
          <a href="/" className="auth-back">
            Voltar para o início
          </a>
        }
      >
        <EmailPasswordReset
          access={access}
          kind="manager"
          onDone={async (email, password) => {
            saveManagerSession(
              await authenticateManager.auth({ email, password }),
            )
            window.location.assign('/gerente')
          }}
          onCancel={() => setForgot(false)}
        />
      </AuthLayout>
    )
  }

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
      alternative={
        emailReset && (
          <button type="button" onClick={() => setForgot(true)}>
            Esqueci a senha
          </button>
        )
      }
    />
  )
}
