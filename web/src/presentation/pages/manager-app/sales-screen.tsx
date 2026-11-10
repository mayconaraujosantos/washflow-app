import { useCallback, useEffect, useState } from 'react'
import { refundableByMethod, type SaleModel } from '@/domain/models/sale-model'
import { manausDate } from '@/main/config/business-time'
import { PAYMENT_LABELS } from '@/main/config/labels'
import { UiIcon } from '@/presentation/components/ui-icon'
import { ScreenLayout } from '@/presentation/components/ui/app-chrome'
import { EmptyState, Notice } from '@/presentation/components/ui/blocks'
import {
  errorMessage,
  formatDateTime,
  formatMoney,
} from '@/presentation/helpers/format'
import { useManager } from './manager-context'
import { SaleRefundSheet } from './sale-refund-sheet'

/** "Vendas realizadas": quick sales by day, with total or partial refunds. */
export function SalesScreen() {
  const { operations, notify } = useManager()
  const [from, setFrom] = useState(manausDate)
  const [to, setTo] = useState(manausDate)
  const [sales, setSales] = useState<SaleModel[] | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [refunding, setRefunding] = useState<SaleModel | null>(null)
  const [busyId, setBusyId] = useState<string | null>(null)

  const search = useCallback(
    async (start: string, end: string) => {
      setLoading(true)
      setError(null)
      try {
        setSales(await operations.listSales(start, end, 'QUICK_SALE'))
      } catch (cause) {
        setSales(null)
        setError(errorMessage(cause, 'Falha ao carregar as vendas'))
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
      .listSales(today, today, 'QUICK_SALE')
      .then((rows) => {
        if (active) setSales(rows)
      })
      .catch((cause) => {
        if (active) setError(errorMessage(cause, 'Falha ao carregar as vendas'))
      })
    return () => {
      active = false
    }
  }, [operations])

  return (
    <ScreenLayout title="Vendas realizadas" backTo="/gerente">
      <form
        className="form"
        onSubmit={(event) => {
          event.preventDefault()
          void search(from, to)
        }}
      >
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
          <UiIcon name="search" size={16} />
          {loading ? 'Buscando...' : 'Buscar'}
        </button>
      </form>
      <Notice tone="error">{error}</Notice>
      {sales &&
        (sales.length === 0 ? (
          <EmptyState icon="cart" title="Nenhuma venda neste período." />
        ) : (
          <ul className="list">
            {sales.map((sale) => {
              const refundable =
                Object.keys(refundableByMethod(sale)).length > 0
              return (
                <li key={sale.id} className="order-card">
                  <div className="order-head">
                    <strong>{sale.customerName ?? 'Venda balcão'}</strong>
                    <strong>{formatMoney(sale.total)}</strong>
                  </div>
                  <p className="muted">
                    {sale.items
                      .map((item) => `${item.quantity}× ${item.description}`)
                      .join(' · ')}
                  </p>
                  <div className="order-meta">
                    <span>
                      <UiIcon name="clock" size={15} />
                      {formatDateTime(sale.createdAt)}
                    </span>
                    <span>
                      <UiIcon name="money" size={15} />
                      {sale.payments
                        .map(
                          (payment) =>
                            `${PAYMENT_LABELS[payment.method]} ${formatMoney(payment.amount)}`,
                        )
                        .join(' + ') || 'Sem pagamento'}
                    </span>
                  </div>
                  {sale.refundedAmount > 0 && (
                    <p className="error-message">
                      {`Estornado ${formatMoney(sale.refundedAmount)}`}
                    </p>
                  )}
                  {refundable && (
                    <div className="order-actions">
                      <button
                        type="button"
                        className="btn btn-ghost"
                        disabled={busyId === sale.id}
                        onClick={() => setRefunding(sale)}
                      >
                        <UiIcon name="money" size={18} />
                        Estornar
                      </button>
                    </div>
                  )}
                </li>
              )
            })}
          </ul>
        ))}
      <SaleRefundSheet
        key={refunding?.id ?? 'none'}
        sale={refunding}
        onClose={() => setRefunding(null)}
        onConfirm={(sale, params) => {
          setRefunding(null)
          setBusyId(sale.id)
          operations
            .refundSale(sale.id, params)
            .then((updated) => {
              setSales((current) =>
                (current ?? []).map((item) =>
                  item.id === updated.id ? updated : item,
                ),
              )
              notify(
                'success',
                `Estorno de ${formatMoney(params.amount)} registrado.`,
              )
            })
            .catch((cause) =>
              notify('error', errorMessage(cause, 'Falha ao estornar')),
            )
            .finally(() => setBusyId(null))
        }}
      />
    </ScreenLayout>
  )
}
