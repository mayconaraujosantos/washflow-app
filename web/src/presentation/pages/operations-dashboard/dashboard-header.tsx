import { useNavigate } from 'react-router-dom'
import { UiIcon } from '@/presentation/components/ui-icon'
import {
  clearManagerSession,
  loadManagerSession,
} from '@/main/session/manager-session'
import {
  clearWasherSession,
  loadWasherSession,
} from '@/main/session/washer-session'
import type { Operations } from '@/domain/usecases/operations'
import type { Features } from '@/domain/usecases/features'

type Props = Readonly<{
  role: 'washer' | 'manager'
  operations: Operations
  features: Features
}>

export function DashboardHeader({ role }: Props) {
  const navigate = useNavigate()
  const session = role === 'washer' ? loadWasherSession() : loadManagerSession()
  const user = session?.user

  const logout = () => {
    if (role === 'washer') {
      clearWasherSession()
      navigate('/login/lavador')
    } else {
      clearManagerSession()
      navigate('/login/gerente')
    }
  }

  return (
    <header className="dashboard-header">
      <div>
        <span className="badge">
          <UiIcon name={role === 'washer' ? 'washer' : 'manager'} size={15} />
          Washflow · {role === 'washer' ? 'Lavador' : 'Gerente'}
        </span>
        <h1>Olá, {user?.name.split(' ')[0]}</h1>
        <p className="dashboard-subtitle">
          {role === 'washer'
            ? 'Acompanhe a fila e organize suas lavagens.'
            : 'Visão geral da operação e dos atendimentos.'}
        </p>
      </div>
      <button className="secondary-btn" onClick={logout}>
        <UiIcon name="logout" size={16} />
        Sair
      </button>
    </header>
  )
}
