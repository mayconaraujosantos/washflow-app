import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { LoadHealthStatus } from '@/domain/usecases/load-health-status'
import { UiIcon, type UiIconName } from '@/presentation/components/ui-icon'
import './home.css'

type Props = {
  loadHealthStatus: LoadHealthStatus
}

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
    description: 'Gerencie o pátio e os atendimentos.',
    icon: 'manager',
  },
] as const

export function Home({ loadHealthStatus }: Props) {
  const [status, setStatus] = useState('checking...')

  useEffect(() => {
    loadHealthStatus
      .load()
      .then((result) => setStatus(result.status))
      .catch(() => setStatus('offline'))
  }, [loadHealthStatus])

  return (
    <main className="app-shell">
      <section className="card">
        <div className="home-brand">
          <span className="home-brand-mark">
            <UiIcon name="sparkles" size={25} />
          </span>
          <div>
            <span className="badge">Gestão inteligente de lavagem</span>
            <h1>Washflow</h1>
          </div>
        </div>
        <p className="subtitle">Tudo em ordem. Seu veículo, também.</p>
        <p className="home-instructions">
          Escolha seu perfil para continuar ou abrir o QR Code de acesso.
        </p>

        <div className="status-row">
          <span
            className={`status-dot${status === 'offline' ? ' status-dot-offline' : ''}`}
            aria-hidden="true"
          />
          <span>
            {status === 'checking...' ? 'Verificando sistema' : status}
          </span>
        </div>

        <div className="profile-grid">
          {PROFILES.map(({ slug, label, description, icon }) => (
            <article className="profile-card" key={slug}>
              <span className="profile-icon">
                <UiIcon name={icon} size={22} />
              </span>
              <h2>{label}</h2>
              <p>{description}</p>
              <div className="profile-actions">
                <Link
                  to={`/qrcodes/${slug}`}
                  className="profile-link-primary"
                  aria-label={`Ver QR Code de ${label}`}
                >
                  <UiIcon name="calendar" size={16} />
                  QR Code
                </Link>
                <Link
                  to={`/login/${slug}`}
                  className="profile-link-secondary"
                  aria-label={`Entrar como ${label}`}
                >
                  Entrar
                  <span aria-hidden="true">→</span>
                </Link>
              </div>
            </article>
          ))}
        </div>

        <div className="actions">
          <span className="home-footer-note">Acesso seguro por perfil</span>
          <div className="home-footer-links">
            <a href="/api/health">Health check</a>
            <a href="/api/hello">API hello</a>
          </div>
        </div>
      </section>
    </main>
  )
}
