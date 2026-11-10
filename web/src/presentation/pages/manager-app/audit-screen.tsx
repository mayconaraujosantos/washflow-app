import { useCallback, useEffect, useState } from 'react'
import type { AuditEntryModel } from '@/domain/models/audit-entry-model'
import { manausDate } from '@/main/config/business-time'
import { UiIcon } from '@/presentation/components/ui-icon'
import { ScreenLayout } from '@/presentation/components/ui/app-chrome'
import { EmptyState, Notice } from '@/presentation/components/ui/blocks'
import { errorMessage, formatDateTime } from '@/presentation/helpers/format'
import { auditDetails, describeAuditAction } from './audit-description'
import { useManager } from './manager-context'

const PROFILE_LABELS: Record<string, string> = {
  MANAGER: 'Gerente',
  WASHER: 'Lavador',
  CUSTOMER: 'Cliente',
  ADMIN: 'Administrador',
}

/** "Histórico de alterações": who changed what and when, for cash disputes. */
export function AuditScreen() {
  const { operations } = useManager()
  const [from, setFrom] = useState(manausDate)
  const [to, setTo] = useState(manausDate)
  const [entries, setEntries] = useState<AuditEntryModel[] | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const search = useCallback(
    async (start: string, end: string) => {
      setLoading(true)
      setError(null)
      try {
        setEntries(await operations.loadAudit(start, end))
      } catch (cause) {
        setEntries(null)
        setError(errorMessage(cause, 'Falha ao carregar o histórico'))
      } finally {
        setLoading(false)
      }
    },
    [operations],
  )

  useEffect(() => {
    const today = manausDate()
    let active = true
    operations
      .loadAudit(today, today)
      .then((rows) => {
        if (active) setEntries(rows)
      })
      .catch((cause) => {
        if (active)
          setError(errorMessage(cause, 'Falha ao carregar o histórico'))
      })
    return () => {
      active = false
    }
  }, [operations])

  return (
    <ScreenLayout title="Histórico de alterações" backTo="/gerente">
      <form
        className="form"
        onSubmit={(event) => {
          event.preventDefault()
          void search(from, to)
        }}
      >
        <p className="muted">
          Quem lançou, alterou ou excluiu o quê, no horário de Manaus. Senhas e
          códigos nunca aparecem.
        </p>
        <div className="report-filters">
          <label>
            De
            <input
              type="date"
              value={from}
              max={to}
              onChange={(event) => setFrom(event.target.value)}
            />
          </label>
          <label>
            Até
            <input
              type="date"
              value={to}
              min={from}
              onChange={(event) => setTo(event.target.value)}
            />
          </label>
        </div>
        <button type="submit" className="btn btn-accent" disabled={loading}>
          <UiIcon name="list" size={16} />
          {loading ? 'Buscando...' : 'Buscar'}
        </button>
      </form>
      <Notice tone="error">{error}</Notice>
      {entries &&
        (entries.length === 0 ? (
          <EmptyState icon="list" title="Nenhuma alteração neste período." />
        ) : (
          <ul className="list">
            {entries.map((entry) => {
              const details = auditDetails(entry.details)
              return (
                <li
                  key={`${entry.at}-${entry.path}-${entry.userId}`}
                  className="order-card"
                >
                  <div className="order-head">
                    <strong>
                      {describeAuditAction(entry.method, entry.route)}
                    </strong>
                  </div>
                  <div className="order-meta">
                    <span>
                      <UiIcon name="clock" size={15} />
                      {formatDateTime(entry.at)}
                    </span>
                    <span>
                      <UiIcon name="manager" size={15} />
                      {entry.userName ?? 'Conta removida'} (
                      {PROFILE_LABELS[entry.profile] ?? entry.profile})
                    </span>
                  </div>
                  {details && <p className="muted">{details}</p>}
                </li>
              )
            })}
          </ul>
        ))}
    </ScreenLayout>
  )
}
