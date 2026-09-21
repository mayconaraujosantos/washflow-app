import { SubmitEvent, useState } from 'react'
import { UserModel } from '@/domain/models/user-model'
import { AuthenticateWasher } from '@/domain/usecases/authenticate-washer'
import { saveWasherSession } from '@/main/session/washer-session'
import './washer-login.css'

type Props = Readonly<{
  authenticateWasher: AuthenticateWasher
}>

// There's no LAVADOR dashboard yet (only /cliente exists so far), so a
// successful login shows an inline confirmation instead of navigating
// somewhere that doesn't exist. Swap this for a redirect once that panel
// ships.
export function WasherLogin({ authenticateWasher }: Props) {
  const [name, setName] = useState('')
  const [phone, setPhone] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [washer, setWasher] = useState<UserModel | null>(null)

  const handleSubmit = async (event: SubmitEvent) => {
    event.preventDefault()
    setError(null)
    setLoading(true)

    try {
      const user = await authenticateWasher.auth({ name, phone })
      saveWasherSession(user)
      setWasher(user)
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
          <p className="subtitle">
            Bem-vindo, {washer.name}! Painel do lavador em breve.
          </p>
        ) : (
          <>
            <p className="subtitle">
              Informe seu nome e telefone para começar seu turno.
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
