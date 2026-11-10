import { useCallback, useEffect, useState } from 'react'
import type { UserModel } from '@/domain/models/user-model'
import { manausDate } from '@/main/config/business-time'
import { CashManager } from '@/presentation/components/cash-manager'
import { ScreenLayout } from '@/presentation/components/ui/app-chrome'
import { Notice } from '@/presentation/components/ui/blocks'
import { errorMessage, formatMoney } from '@/presentation/helpers/format'
import { OperationsReportSection } from '@/presentation/pages/operations-dashboard/operations-report-section'
import { TeamManagementSection } from '@/presentation/pages/operations-dashboard/team-management-section'
import { BadgeSheet } from './access-sheets'
import { useManager } from './manager-context'

type DaySummary = {
  ordersRevenue: number
  salesReceived: number
  cashIncome: number
  cashExpense: number
  refunds: number
}

/**
 * "Caixa do dia": what came in today (orders + counter sales + cash entries) minus expenses and
 * the refunds made today (of orders and sales from any day).
 */
export function CashScreen() {
  const { operations, features } = useManager()
  const [summary, setSummary] = useState<DaySummary | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const today = manausDate()
    let active = true
    Promise.all([
      operations.loadReport(today, today),
      operations.listSales(today, today),
      features.transactionReport(today, today),
      operations.listSaleRefunds(today, today),
    ])
      .then(([report, sales, cash, saleRefunds]) => {
        if (!active) return
        setSummary({
          ordersRevenue: Number(report.revenue),
          salesReceived: sales.reduce(
            (sum, sale) => sum + Number(sale.paidAmount),
            0,
          ),
          cashIncome: Number(cash.totalIncome),
          cashExpense: Number(cash.totalExpense),
          refunds:
            Number(report.refunds ?? 0) +
            saleRefunds.reduce((sum, refund) => sum + Number(refund.amount), 0),
        })
      })
      .catch((cause) => {
        if (active) setError(errorMessage(cause, 'Falha ao carregar o caixa'))
      })
    return () => {
      active = false
    }
  }, [operations, features])

  const balance = summary
    ? summary.ordersRevenue +
      summary.salesReceived +
      summary.cashIncome -
      summary.cashExpense -
      summary.refunds
    : 0

  return (
    <ScreenLayout title="Caixa do dia" backTo="/gerente">
      <Notice tone="error">{error}</Notice>
      {summary && (
        <section className="card">
          <div className="report-metrics">
            <div className="report-card income">
              <span>Atendimentos</span>
              <strong>{formatMoney(summary.ordersRevenue)}</strong>
            </div>
            <div className="report-card income">
              <span>Vendas rápidas</span>
              <strong>{formatMoney(summary.salesReceived)}</strong>
            </div>
            <div className="report-card income">
              <span>Outras entradas</span>
              <strong>{formatMoney(summary.cashIncome)}</strong>
            </div>
            <div className="report-card expense">
              <span>Despesas</span>
              <strong>{formatMoney(summary.cashExpense)}</strong>
            </div>
            <div className="report-card expense">
              <span>Estornos</span>
              <strong>{formatMoney(summary.refunds)}</strong>
            </div>
          </div>
          <div className="kpi-band">
            <div>
              <span>Saldo do dia</span>
              <strong>{formatMoney(balance)}</strong>
            </div>
          </div>
        </section>
      )}
      <section className="card">
        <h2>Lançar entrada ou despesa</h2>
        <CashManager features={features} />
      </section>
    </ScreenLayout>
  )
}

export function TeamScreen() {
  const { user, operations } = useManager()
  const [users, setUsers] = useState<UserModel[]>([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [busyUserId, setBusyUserId] = useState<string | null>(null)
  const [badgeFor, setBadgeFor] = useState<UserModel | null>(null)

  const reload = useCallback(() => {
    operations
      .loadUsers()
      .then((accounts) => {
        setUsers(accounts)
        setLoadError(null)
      })
      .catch((cause) =>
        setLoadError(
          errorMessage(cause, 'Não foi possível carregar as contas'),
        ),
      )
      .finally(() => setLoading(false))
  }, [operations])

  useEffect(() => {
    reload()
  }, [reload])

  return (
    <ScreenLayout title="Equipe e acessos" backTo="/gerente">
      <Notice tone="error">{error}</Notice>
      <Notice tone="success">{notice}</Notice>
      <TeamManagementSection
        users={users}
        currentUser={user}
        operations={operations}
        teamLoading={loading}
        teamError={loadError}
        busyUserId={busyUserId}
        setBusyUserId={setBusyUserId}
        setNotice={setNotice}
        setError={setError}
        onChanged={reload}
        onIssueBadge={setBadgeFor}
      />
      <BadgeSheet washer={badgeFor} onClose={() => setBadgeFor(null)} />
    </ScreenLayout>
  )
}

export function ReportScreen() {
  const { operations } = useManager()
  return (
    <ScreenLayout title="Relatórios" backTo="/gerente">
      <OperationsReportSection operations={operations} />
    </ScreenLayout>
  )
}
