import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { LoadHealthStatus } from '@/domain/usecases/load-health-status'
import { UiIcon, type UiIconName } from '@/presentation/components/ui-icon'
import { BrandLogo } from '@/presentation/components/ui/app-chrome'

type Props = Readonly<{
  loadHealthStatus: LoadHealthStatus
}>

type Role = {
  slug: string
  label: string
  description: string
  icon: UiIconName
}

// One card per perfil (usuarios.perfil in CLAUDE.md). "Entrar" is the main
// action; the QR code is a shortcut meant for the shop's counter screen.
const ROLES: Role[] = [
  {
    slug: 'cliente',
    label: 'Cliente',
    description: 'Agende e acompanhe a lavagem do seu veículo.',
    icon: 'car',
  },
  {
    slug: 'lavador',
    label: 'Lavador',
    description: 'Veja a fila do pátio e conduza suas lavagens.',
    icon: 'washer',
  },
  {
    slug: 'gerente',
    label: 'Gerente',
    description: 'Entradas, pátio, agenda, vendas e caixa.',
    icon: 'manager',
  },
]

type Health = 'checking' | 'online' | 'offline'

const HEALTH_LABELS: Record<Health, string> = {
  checking: 'Verificando',
  online: 'Online',
  offline: 'Offline',
}

/** Profile choice: each role card carries its own actions (single-step access). */
export function Home({ loadHealthStatus }: Props) {
  const [health, setHealth] = useState<Health>('checking')

  useEffect(() => {
    let active = true
    loadHealthStatus
      .load()
      .then((result) => {
        if (active) setHealth(result.status === 'ok' ? 'online' : 'offline')
      })
      .catch(() => {
        if (active) setHealth('offline')
      })
    return () => {
      active = false
    }
  }, [loadHealthStatus])

  return (
    <main className="role-screen">
      <header className="role-header">
        <h1 className="auth-brand">
          <BrandLogo size="lg" />
        </h1>
        <p className="role-slogan">Tudo em ordem. Seu veículo, também.</p>
        <span className={`status-chip is-${health}`} role="status">
          <span className="status-chip-dot" aria-hidden="true" />
          Sistema {HEALTH_LABELS[health].toLowerCase()}
        </span>
      </header>

      <ul className="role-list" aria-label="Escolha seu perfil">
        {ROLES.map(({ slug, label, description, icon }) => (
          <li key={slug} className="role-card">
            <span className="role-icon" aria-hidden="true">
              <UiIcon name={icon} size={26} />
            </span>
            <div className="role-text">
              <h2>{label}</h2>
              <p>{description}</p>
            </div>
            <div className="role-actions">
              <Link
                to={`/login/${slug}`}
                className="role-btn role-btn-primary"
                aria-label={`Entrar como ${label}`}
              >
                Entrar
              </Link>
              <Link
                to={`/qrcodes/${slug}`}
                className="role-btn role-btn-secondary"
                aria-label={`Ver QR Code de ${label}`}
              >
                <UiIcon name="qr" size={18} />
                QR Code
              </Link>
            </div>
          </li>
        ))}
      </ul>
    </main>
  )
}
