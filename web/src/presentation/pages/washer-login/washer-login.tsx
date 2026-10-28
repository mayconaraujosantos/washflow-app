import { SubmitEvent, useState } from 'react'
import { UserModel } from '@/domain/models/user-model'
import { AuthenticateWasher } from '@/domain/usecases/authenticate-washer'
import { saveWasherSession } from '@/main/session/washer-session'
import './washer-login.css'

type Props = Readonly<{
  authenticateWasher: AuthenticateWasher
}>

export function WasherLogin({ authenticateWasher }: Props) {
  const [phone, setPhone] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [washer, setWasher] = useState<UserModel | null>(null)

  const handleSubmit = async (event: SubmitEvent) => {
    event.preventDefault()
    setError(null)
    setLoading(true)

    try {
      const session = await authenticateWasher.auth({ phone, password })
      saveWasherSession(session)
      setWasher(session.user)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao entrar')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="login-shell">
      <section className="login-card">
        <span className="badge">Washflow · Lavador</span>
        <h1>Entrar</h1>

        {washer ? (
          <div className="subtitle">
            <p>Bem-vindo, {washer.name}!</p>
            <a href="/lavador">Abrir painel do lavador</a>
          </div>
        ) : (
          <>
            <p className="subtitle">
              Entre com o telefone e a senha fornecidos pelo gerente.
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
                <span>Senha (mínimo 8 caracteres)</span>
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
