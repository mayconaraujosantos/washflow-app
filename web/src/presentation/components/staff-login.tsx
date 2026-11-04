import { SubmitEvent, useState, type ReactNode } from 'react'
import type { AuthenticatedSessionModel } from '@/domain/models/authenticated-session-model'
import type { UserModel } from '@/domain/models/user-model'
import { AuthLayout } from '@/presentation/components/ui/app-chrome'
import {
  PasswordField,
  PhoneField,
  TextField,
} from '@/presentation/components/ui/auth-fields'
import { Notice } from '@/presentation/components/ui/blocks'
import { errorMessage } from '@/presentation/helpers/format'

type Props = Readonly<{
  title: string
  subtitle: string
  passwordLabel: string
  panelPath: string
  panelLabel: string
  // 'emailOrPhone' (managers): one field; anything with "@" is sent as e-mail.
  identifier?: 'phone' | 'emailOrPhone'
  authenticate: (credentials: Credentials) => Promise<AuthenticatedSessionModel>
  saveSession: (session: AuthenticatedSessionModel) => void
  // Extra link under the form (e.g. "Entrar com PIN ou crachá").
  alternative?: ReactNode
}>

export type Credentials =
  { email: string; password: string } | { phone: string; password: string }

/**
 * Washer and manager login: phone + password of an account provisioned by a manager. The
 * profile is fixed by which page (and API endpoint) is used, never chosen by the user.
 */
export function StaffLogin({
  title,
  subtitle,
  passwordLabel,
  panelPath,
  panelLabel,
  identifier = 'phone',
  authenticate,
  saveSession,
  alternative,
}: Props) {
  const [phone, setPhone] = useState('')
  const [login, setLogin] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [user, setUser] = useState<UserModel | null>(null)

  const credentials = (): Credentials => {
    if (identifier === 'phone') return { phone, password }
    const typed = login.trim()
    return typed.includes('@')
      ? { email: typed.toLowerCase(), password }
      : { phone: typed.replace(/\D/g, ''), password }
  }

  const handleSubmit = async (event: SubmitEvent) => {
    event.preventDefault()
    setError(null)
    setLoading(true)
    try {
      const session = await authenticate(credentials())
      saveSession(session)
      setUser(session.user)
    } catch (cause) {
      setError(errorMessage(cause, 'Erro ao entrar'))
    } finally {
      setLoading(false)
    }
  }

  if (user) {
    return (
      <AuthLayout title={`Bem-vindo, ${user.name}!`}>
        <a href={panelPath} className="btn btn-accent btn-block">
          {panelLabel}
        </a>
      </AuthLayout>
    )
  }

  return (
    <AuthLayout
      title={title}
      subtitle={subtitle}
      footer={
        <a href="/" className="auth-back">
          Voltar para o início
        </a>
      }
    >
      <form className="form" onSubmit={handleSubmit}>
        {identifier === 'phone' ? (
          <PhoneField value={phone} onChange={setPhone} />
        ) : (
          <TextField
            label="E-mail ou telefone"
            value={login}
            onChange={setLogin}
            autoComplete="username"
            inputMode="email"
          />
        )}
        <PasswordField
          label={passwordLabel}
          value={password}
          onChange={setPassword}
        />
        <Notice tone="error">{error}</Notice>
        <button type="submit" className="btn btn-dark" disabled={loading}>
          {loading ? 'Entrando...' : 'Entrar'}
        </button>
        {alternative && <div className="auth-links">{alternative}</div>}
      </form>
    </AuthLayout>
  )
}
