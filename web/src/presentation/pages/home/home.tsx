import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { LoadHealthStatus } from '@/domain/usecases/load-health-status'
import { UiIcon, type UiIconName } from '@/presentation/components/ui-icon'
import { BrandLogo } from '@/presentation/components/ui/app-chrome'

type Props = Readonly<{
  loadHealthStatus: LoadHealthStatus
}>

// One QR code and one direct login link per perfil (usuarios.perfil in
// CLAUDE.md) - a QR code is meant for the shop's own screen/counter, the
// login link for anyone who already knows the URL (e.g. bookmarked on a
// staff phone) and doesn't want to scan anything.
const PROFILES: {
  slug: string
  label: string
  description: string
  icon: UiIconName
}[] = [
  {
    slug: 'cliente',
    label: 'Cliente',
    description: 'Agende e acompanhe seu veículo.',
    icon: 'car',
  },
  {
    slug: 'lavador',
    label: 'Lavador',
    description: 'Acesse sua fila e suas lavagens.',
    icon: 'washer',
  },
  {
    slug: 'gerente',
    label: 'Gerente',
    description: 'Entradas, pátio, agenda, vendas e caixa.',
    icon: 'manager',
  },
]

export function Home({ loadHealthStatus }: Props) {
  const [status, setStatus] = useState('checking...')

  useEffect(() => {
    loadHealthStatus
      .load()
      .then((result) => setStatus(result.status))
      .catch(() => setStatus('offline'))
  }, [loadHealthStatus])

  return (
    <main className="auth-shell">
      <h1>
        <BrandLogo size="lg" />
      </h1>
      <p className="muted">Tudo em ordem. Seu veículo, também.</p>

      <div className="status-row">
        <span
          className={`status-dot${status === 'offline' ? ' status-dot-offline' : ''}`}
          aria-hidden="true"
        />
        <span>{status === 'checking...' ? 'Verificando sistema' : status}</span>
      </div>

      <div className="profile-grid">
        {PROFILES.map(({ slug, label, description, icon }) => (
          <article className="profile-card" key={slug}>
            <span className="profile-icon">
              <UiIcon name={icon} size={24} />
            </span>
            <h2>{label}</h2>
            <p>{description}</p>
            <div className="profile-actions">
              <Link
                to={`/qrcodes/${slug}`}
                className="btn btn-outline btn-small"
                aria-label={`Ver QR Code de ${label}`}
              >
                QR Code
              </Link>
              <Link
                to={`/login/${slug}`}
                className="btn btn-accent btn-small"
                aria-label={`Entrar como ${label}`}
              >
                Entrar
              </Link>
            </div>
          </article>
        ))}
      </div>
    </main>
  )
}
