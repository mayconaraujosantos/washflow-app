import { SubmitEvent, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AuthenticateCustomer } from '@/domain/usecases/authenticate-customer'
import { saveCustomerSession } from '@/main/session/customer-session'
import './customer-login.css'

type Props = Readonly<{
  authenticateCustomer: AuthenticateCustomer
}>

export function CustomerLogin({ authenticateCustomer }: Props) {
  const navigate = useNavigate()
  const [name, setName] = useState('')
  const [phone, setPhone] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (event: SubmitEvent) => {
    event.preventDefault()
    setError(null)
    setLoading(true)

    try {
      const user = await authenticateCustomer.auth({ name, phone })
      saveCustomerSession(user)
      navigate('/cliente')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao entrar')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="login-shell">
      <section className="login-card">
        <span className="badge">Washflow · Cliente</span>
        <h1>Entrar</h1>
        <p className="subtitle">
          Informe seu nome e telefone para agendar sua lavagem.
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
      </section>
    </main>
  )
}
