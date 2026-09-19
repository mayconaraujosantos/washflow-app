import { useEffect, useState } from 'react'
import { LoadHealthStatus } from '@/domain/usecases/load-health-status'
import './home.css'

type Props = {
  loadHealthStatus: LoadHealthStatus
}

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
          React + Javalin working together as a progressive web app.
        </p>

        <div className="status-row">
          <span className="status-dot" aria-hidden="true" />
          <span>{status}</span>
        </div>

        <div className="actions">
          <a href="/api/health" className="primary-btn">
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
