import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { LoadHealthStatus } from '@/domain/usecases/load-health-status'
import './home.css'

type Props = {
  loadHealthStatus: LoadHealthStatus
}

// One QR code and one direct login link per perfil (usuarios.perfil in
// CLAUDE.md) - a QR code is meant for the shop's own screen/counter, the
// login link for anyone who already knows the URL (e.g. bookmarked on a
// staff phone) and doesn't want to scan anything.
const PROFILES = [
  { slug: 'cliente', label: 'Cliente' },
  { slug: 'lavador', label: 'Lavador' },
  { slug: 'gerente', label: 'Gerente' },
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
        <span className="badge">PWA Ready</span>
        <h1>Washflow</h1>
        <p className="subtitle">
          Escolha seu perfil para escanear o QR Code ou entrar direto.
        </p>

        <div className="status-row">
          <span className="status-dot" aria-hidden="true" />
          <span>{status}</span>
        </div>

        <div className="profile-grid">
          {PROFILES.map(({ slug, label }) => (
            <article className="profile-card" key={slug}>
              <h2>{label}</h2>
              <div className="profile-actions">
                <Link
                  to={`/qrcodes/${slug}`}
                  className="profile-link-primary"
                  aria-label={`Ver QR Code de ${label}`}
                >
                  Ver QR Code
                </Link>
                <Link
                  to={`/login/${slug}`}
                  className="profile-link-secondary"
                  aria-label={`Entrar como ${label}`}
                >
                  Entrar
                </Link>
              </div>
            </article>
          ))}
        </div>

        <div className="actions">
          <a href="/api/health" className="secondary-btn">
            Health check
          </a>
          <a href="/api/hello" className="secondary-btn">
            API hello
          </a>
        </div>
      </section>
    </main>
  )
}
