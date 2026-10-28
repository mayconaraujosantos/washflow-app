import { SubmitEvent, useState } from 'react'
import { UserModel } from '@/domain/models/user-model'
import { AuthenticateManager } from '@/domain/usecases/authenticate-manager'
import { saveManagerSession } from '@/main/session/manager-session'
import './manager-login.css'

type Props = Readonly<{
  authenticateManager: AuthenticateManager
}>

export function ManagerLogin({ authenticateManager }: Props) {
  const [phone, setPhone] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [manager, setManager] = useState<UserModel | null>(null)

  const handleSubmit = async (event: SubmitEvent) => {
    event.preventDefault()
    setError(null)
    setLoading(true)

    try {
      const session = await authenticateManager.auth({ phone, password })
      saveManagerSession(session)
      setManager(session.user)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao entrar')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="login-shell">
      <section className="login-card">
        <span className="badge">Washflow · Gerente</span>
        <h1>Entrar</h1>

        {manager ? (
          <div className="subtitle">
            <p>Bem-vindo, {manager.name}!</p>
            <a href="/gerente">Abrir painel do gerente</a>
          </div>
        ) : (
          <>
            <p className="subtitle">
              Entre com o telefone e a senha cadastrados como gerente.
            </p>

            <form className="login-form" onSubmit={handleSubmit}>
              <label>
                <span>Telefone</span>
                <input
                  type="tel"
                  value={phone}
                  onChange={(event) => setPhone(event.target.value)}
                  placeholder="11999990000"
                  autoComplete="tel"
                  required
                />
              </label>

              <label>
                <span>Senha</span>
                <input
                  type="password"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  autoComplete="current-password"
                  minLength={8}
                  required
                />
              </label>

              {error && <p className="error-message">{error}</p>}

              <button type="submit" className="primary-btn" disabled={loading}>
                {loading ? 'Entrando...' : 'Entrar'}
              </button>
            </form>
          </>
        )}
      </section>
    </main>
  )
}
