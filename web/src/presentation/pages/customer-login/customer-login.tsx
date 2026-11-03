import { SubmitEvent, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { AuthenticateCustomer } from '@/domain/usecases/authenticate-customer'
import { saveCustomerSession } from '@/main/session/customer-session'
import { AuthLayout } from '@/presentation/components/ui/app-chrome'
import { Notice } from '@/presentation/components/ui/blocks'
import { errorMessage } from '@/presentation/helpers/format'

type Props = Readonly<{
  authenticateCustomer: AuthenticateCustomer
}>

export function CustomerLogin({ authenticateCustomer }: Props) {
  const navigate = useNavigate()
  const [name, setName] = useState('')
  const [phone, setPhone] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (event: SubmitEvent) => {
    event.preventDefault()
    setError(null)
    setLoading(true)
    try {
      const session = await authenticateCustomer.auth({ name, phone, password })
      saveCustomerSession(session)
      navigate('/cliente')
    } catch (cause) {
      setError(errorMessage(cause, 'Erro ao entrar'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthLayout
      title="Entrar ou criar conta"
      subtitle="Informe seu nome e telefone para agendar sua lavagem."
      footer={
        <Link to="/" className="faint">
          Voltar para o início
        </Link>
      }
    >
      <form className="form" onSubmit={handleSubmit}>
        <label className="field">
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
        <Notice tone="error">{error}</Notice>
        <button type="submit" className="btn btn-accent" disabled={loading}>
          {loading ? 'Entrando...' : 'Entrar'}
        </button>
      </form>
    </AuthLayout>
  )
}
