import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ServiceOrderModel } from '@/domain/models/service-order-model'
import { ServicePriceModel } from '@/domain/models/service-price-model'
import { UserModel } from '@/domain/models/user-model'
import type { ChecklistType } from '@/domain/models/feature-models'
import type { Operations } from '@/domain/usecases/operations'
import type { Features } from '@/domain/usecases/features'
import { loadManagerSession } from '@/main/session/manager-session'
import { loadWasherSession } from '@/main/session/washer-session'
import { ChecklistModal } from '@/presentation/components/checklist-modal'
import { CashManager } from '@/presentation/components/cash-manager'
import { DashboardHeader } from './dashboard-header'
import { DashboardMetrics } from './dashboard-metrics'
import { WasherQueueSection } from './washer-queue-section'
import { WasherOrdersSection } from './washer-orders-section'
import { ManagerYardSection } from './manager-yard-section'
import { TeamManagementSection } from './team-management-section'
import { OperationsReportSection } from './operations-report-section'
import './operations-dashboard.css'

type Props = Readonly<{
  role: 'washer' | 'manager'
  operations: Operations
  features: Features
}>

export function OperationsDashboard({ role, operations, features }: Props) {
  const navigate = useNavigate()
  const [session] = useState(() =>
    role === 'washer' ? loadWasherSession() : loadManagerSession(),
  )
  const user = session?.user

  // Data state
  const [waiting, setWaiting] = useState<ServiceOrderModel[]>([])
  const [myOrders, setMyOrders] = useState<ServiceOrderModel[]>([])
  const [yard, setYard] = useState<ServiceOrderModel[]>([])
  const [servicePrices, setServicePrices] = useState<ServicePriceModel[]>([])
  const [users, setUsers] = useState<UserModel[]>([])

  // UI state
  const [teamLoading, setTeamLoading] = useState(role === 'manager')
  const [teamError, setTeamError] = useState<string | null>(null)
  const [checklistModal, setChecklistModal] = useState<{
    serviceOrderId: string
    type: ChecklistType
  } | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [busyOrderId, setBusyOrderId] = useState<string | null>(null)
  const [busyUserId, setBusyUserId] = useState<string | null>(null)

  // Refresh data
  const refresh = useCallback(async () => {
    try {
      if (role === 'washer') {
        const [queue, active] = await Promise.all([
          operations.loadQueue(),
          operations.loadMyOrders(),
        ])
        setWaiting(queue)
        setMyOrders(active)
      } else {
        const [orders, prices] = await Promise.all([
          operations.loadYard(),
          operations.loadServicePrices(),
        ])
        setYard(orders)
        setServicePrices(prices)
      }
      setError(null)
    } catch (cause) {
      setError(
        cause instanceof Error ? cause.message : 'Falha ao atualizar o painel',
      )
    }
  }, [role, operations])

  useEffect(() => {
    if (!user) {
      navigate(`/login/${role === 'washer' ? 'lavador' : 'gerente'}`, {
        replace: true,
      })
      return
    }
    const initialRefresh = window.setTimeout(() => void refresh(), 0)
    const timer = window.setInterval(() => void refresh(), 3000)
    return () => {
      window.clearTimeout(initialRefresh)
      window.clearInterval(timer)
    }
  }, [user, role, navigate, refresh])

  // Load users for manager
  useEffect(() => {
    if (role !== 'manager' || !user) return
    operations
      .loadUsers()
      .then(setUsers)
      .catch((cause) =>
        setTeamError(
          cause instanceof Error
            ? cause.message
            : 'Não foi possível carregar as contas',
        ),
      )
      .finally(() => setTeamLoading(false))
  }, [operations, role, user])

  // Metrics
  const washerMetrics = {
    type: 'washer' as const,
    waitingCount: waiting.length,
    myOrdersCount: myOrders.length,
    pausedCount: myOrders.filter((o) => o.status === 'PAUSED').length,
  }

  const managerMetrics = {
    type: 'manager' as const,
    yardCount: yard.length,
    washingCount: yard.filter((o) => o.status === 'WASHING').length,
    readyCount: yard.filter((o) => o.status === 'READY').length,
  }

  return (
    <main className="dashboard-shell">
      <div
        className={`dashboard-container${role === 'manager' ? ' dashboard-container-manager' : ''}`}
      >
        <DashboardHeader role={role} operations={operations} features={features} />

        {role === 'washer' ? (
          <DashboardMetrics metrics={washerMetrics} />
        ) : (
          <DashboardMetrics metrics={managerMetrics} />
        )}

        {error && (
          <p className="error-message" role="alert">
            {error}
          </p>
        )}
        {notice && (
          <p className="success-message" role="status">
            {notice}
          </p>
        )}

        {role === 'washer' ? (
          <>
            <WasherQueueSection
              waiting={waiting}
              operations={operations}
              userId={user!.id}
              busyOrderId={busyOrderId}
              setBusyOrderId={setBusyOrderId}
              setNotice={setNotice}
              setError={setError}
              refresh={refresh}
            />
            <WasherOrdersSection
              myOrders={myOrders}
              operations={operations}
              busyOrderId={busyOrderId}
              setBusyOrderId={setBusyOrderId}
              setNotice={setNotice}
              setError={setError}
              refresh={refresh}
            />
          </>
        ) : (
          <>
            <ManagerYardSection
              yard={yard}
              servicePrices={servicePrices}
              operations={operations}
              busyOrderId={busyOrderId}
              setBusyOrderId={setBusyOrderId}
              setNotice={setNotice}
              setError={setError}
              refresh={refresh}
            />
            <TeamManagementSection
              users={users}
              currentUser={user!}
              operations={operations}
              teamLoading={teamLoading}
              teamError={teamError}
              busyUserId={busyUserId}
              setBusyUserId={setBusyUserId}
              setNotice={setNotice}
              setError={setError}
            />
            <OperationsReportSection operations={operations} />
            <section className="dashboard-section">
              <CashManager features={features} />
            </section>
          </>
        )}
      </div>
      {checklistModal && (
        <ChecklistModal
          features={features}
          serviceOrderId={checklistModal.serviceOrderId}
          type={checklistModal.type}
          onClose={() => setChecklistModal(null)}
        />
      )}
    </main>
  )
}
