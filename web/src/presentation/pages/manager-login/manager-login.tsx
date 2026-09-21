import { SubmitEvent, useState } from 'react'
import { UserModel } from '@/domain/models/user-model'
import { AuthenticateManager } from '@/domain/usecases/authenticate-manager'
import { saveManagerSession } from '@/main/session/manager-session'
import './manager-login.css'

type Props = Readonly<{
  authenticateManager: AuthenticateManager
}>

// There's no GERENTE dashboard yet (only /cliente exists so far), so a
// successful login shows an inline confirmation instead of navigating
// somewhere that doesn't exist. Swap this for a redirect once that panel
// ships. A 403 here means the phone hasn't been promoted to manager yet
// (see ManagerSelfRegistrationNotAllowedError on the backend) - that comes
// back as a regular error message, same as any other failure.
export function ManagerLogin({ authenticateManager }: Props) {
  const [name, setName] = useState('')
  const [phone, setPhone] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [manager, setManager] = useState<UserModel | null>(null)

  const handleSubmit = async (event: SubmitEvent) => {
    event.preventDefault()
    setError(null)
    setLoading(true)

    try {
      const user = await authenticateManager.auth({ name, phone })
      saveManagerSession(user)
      setManager(user)
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
          <p className="subtitle">
            Bem-vindo, {manager.name}! Painel do gerente em breve.
          </p>
        ) : (
          <>
            <p className="subtitle">
              Informe seu nome e telefone cadastrados como gerente.
            </p>

            <form className="login-form" onSubmit={handleSubmit}>
              <label>
                <span>Nome</span>
                <input
                  type="text"
                  value={name}
                  onChange={(event) => setName(event.target.value)}
                  placeholder="Seu nome"
                  autoComplete="name"
                  required
                />
              </label>

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
