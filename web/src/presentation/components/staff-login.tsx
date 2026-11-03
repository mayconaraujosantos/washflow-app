import { SubmitEvent, useState } from 'react'
import type { AuthenticatedSessionModel } from '@/domain/models/authenticated-session-model'
import type { UserModel } from '@/domain/models/user-model'
import { AuthLayout } from '@/presentation/components/ui/app-chrome'
import { Notice } from '@/presentation/components/ui/blocks'
import { errorMessage } from '@/presentation/helpers/format'

type Props = Readonly<{
  title: string
  subtitle: string
  passwordLabel: string
  panelPath: string
  panelLabel: string
  authenticate: (params: {
    phone: string
    password: string
  }) => Promise<AuthenticatedSessionModel>
  saveSession: (session: AuthenticatedSessionModel) => void
}>

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
  authenticate,
  saveSession,
}: Props) {
  const [phone, setPhone] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [user, setUser] = useState<UserModel | null>(null)

  const handleSubmit = async (event: SubmitEvent) => {
    event.preventDefault()
    setError(null)
    setLoading(true)
    try {
      const session = await authenticate({ phone, password })
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
        <a href="/" className="faint">
          Voltar para o início
        </a>
      }
    >
      <form className="form" onSubmit={handleSubmit}>
        <label className="field">
          <span>Telefone</span>
          <input
            type="tel"
            inputMode="tel"
            value={phone}
            onChange={(event) => setPhone(event.target.value)}
            placeholder="(92) 99999-0000"
            autoComplete="tel"
            required
          />
        </label>
        <label className="field">
          <span>{passwordLabel}</span>
          <input
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            autoComplete="current-password"
            minLength={8}
            required
          />
        </label>
        <Notice tone="error">{error}</Notice>
        <button type="submit" className="btn btn-accent" disabled={loading}>
          {loading ? 'Entrando...' : 'Entrar'}
        </button>
      </form>
    </AuthLayout>
  )
}
