import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link, Navigate, Route, Routes, useNavigate } from 'react-router-dom'
import { CreateVehicle } from '@/domain/usecases/create-vehicle'
import { ListServiceOrdersByCustomer } from '@/domain/usecases/list-service-orders-by-customer'
import { ListServicePrices } from '@/domain/usecases/list-service-prices'
import { ListVehiclesByCustomer } from '@/domain/usecases/list-vehicles-by-customer'
import { ScheduleServiceOrder } from '@/domain/usecases/schedule-service-order'
import { ServiceOrderModel } from '@/domain/models/service-order-model'
import { ServicePriceModel } from '@/domain/models/service-price-model'
import { VehicleModel } from '@/domain/models/vehicle-model'
import {
  clearCustomerSession,
  loadCustomerSession,
} from '@/main/session/customer-session'
import { UiIcon } from '@/presentation/components/ui-icon'
import {
  ScreenLayout,
  type NavItem,
} from '@/presentation/components/ui/app-chrome'
import {
  ActionTile,
  KpiBand,
  Notice,
} from '@/presentation/components/ui/blocks'
import {
  firstName,
  formatBusinessHours,
  greeting,
} from '@/presentation/helpers/format'
import type { Establishment } from '@/domain/usecases/establishment'
import { ScheduleSection } from './schedule-section'
import { TrackingSection } from './tracking-section'
import { VehicleSection } from './vehicle-section'

type Props = Readonly<{
  createVehicle: CreateVehicle
  listVehiclesByCustomer: ListVehiclesByCustomer
  listServicePrices: ListServicePrices
  scheduleServiceOrder: ScheduleServiceOrder
  listServiceOrdersByCustomer: ListServiceOrdersByCustomer
  establishment?: Establishment
}>

const NAV: NavItem[] = [
  { to: '/cliente', label: 'Início', icon: 'home', end: true },
  { to: '/cliente/veiculos', label: 'Veículos', icon: 'car' },
  { to: '/cliente/agendamentos', label: 'Agendamentos', icon: 'calendar' },
]

const STATUS_CHANGE_NOTICES: Record<ServiceOrderModel['status'], string> = {
  SCHEDULED: 'Agendado',
  WAITING_IN_YARD: 'Seu veículo chegou ao pátio',
  WASHING: 'Lavagem iniciada',
  PAUSED: 'Lavagem pausada',
  READY: 'Seu veículo está pronto para retirada',
  DONE: 'Atendimento finalizado',
}

const REFRESH_MS = 3000

/** Customer app: home, vehicles, schedule and live tracking (polls every 3s). */
export function CustomerDashboard({
  createVehicle,
  listVehiclesByCustomer,
  listServicePrices,
  scheduleServiceOrder,
  listServiceOrdersByCustomer,
  establishment,
}: Props) {
  const navigate = useNavigate()
  const [session] = useState(() => loadCustomerSession())
  const user = session?.user ?? null

  const [vehicles, setVehicles] = useState<VehicleModel[]>([])
  const [servicePrices, setServicePrices] = useState<ServicePriceModel[]>([])
  const [serviceOrders, setServiceOrders] = useState<ServiceOrderModel[]>([])
  const [loadError, setLoadError] = useState<string | null>(null)
  const [refreshingOrders, setRefreshingOrders] = useState(false)
  const [statusNotice, setStatusNotice] = useState<string | null>(null)
  const [businessHours, setBusinessHours] = useState<string | undefined>()
  const [openOnSunday, setOpenOnSunday] = useState(false)
  const loadDay = useMemo(
    () =>
      establishment
        ? (date: string) => establishment.availability(date)
        : undefined,
    [establishment],
  )
  const previousStatuses = useRef<Map<
    string,
    ServiceOrderModel['status']
  > | null>(null)

  const updateOrders = useCallback(
    (orders: ServiceOrderModel[], announceChanges: boolean) => {
      const previous = previousStatuses.current
      if (announceChanges && previous) {
        const changed = orders.find(
          (order) =>
            previous.has(order.id) && previous.get(order.id) !== order.status,
        )
        if (changed) setStatusNotice(STATUS_CHANGE_NOTICES[changed.status])
      }
      previousStatuses.current = new Map(
        orders.map((order) => [order.id, order.status]),
      )
      setServiceOrders(orders)
    },
    [],
  )

  useEffect(() => {
    if (!user) navigate('/login/cliente', { replace: true })
  }, [user, navigate])

  useEffect(() => {
    if (!establishment) return
    establishment
      .load()
      .then((settings) => {
        setBusinessHours(formatBusinessHours(settings))
        setOpenOnSunday(settings.openOnSunday)
      })
      .catch(() => {
        // The schedule form keeps its default hint; the API still enforces the rules.
      })
  }, [establishment])

  useEffect(() => {
    if (!user) return
    void Promise.all([
      listVehiclesByCustomer.list({ customerId: user.id }),
      listServicePrices.list(),
      listServiceOrdersByCustomer.list({ customerId: user.id }),
    ])
      .then(([vehiclesResult, pricesResult, ordersResult]) => {
        setVehicles(vehiclesResult)
        setServicePrices(pricesResult)
        updateOrders(ordersResult, false)
      })
      .catch(() => setLoadError('Erro ao carregar seus dados'))
  }, [
    user,
    listVehiclesByCustomer,
    listServicePrices,
    listServiceOrdersByCustomer,
    updateOrders,
  ])

  useEffect(() => {
    if (!user) return
    let active = true
    let refreshing = false
    const timer = window.setInterval(() => {
      if (refreshing) return
      refreshing = true
      listServiceOrdersByCustomer
        .list({ customerId: user.id })
        .then((orders) => {
          if (active) updateOrders(orders, true)
        })
        .catch(() => {
          if (active) setLoadError('Erro ao atualizar agendamentos')
        })
        .finally(() => {
          refreshing = false
        })
    }, REFRESH_MS)
    return () => {
      active = false
      window.clearInterval(timer)
    }
  }, [user, listServiceOrdersByCustomer, updateOrders])

  if (!user) return null

  const handleLogout = () => {
    clearCustomerSession()
    navigate('/login/cliente')
  }

  const handleRefreshOrders = () => {
    setRefreshingOrders(true)
    void listServiceOrdersByCustomer
      .list({ customerId: user.id })
      .then((orders) => updateOrders(orders, true))
      .catch(() => setLoadError('Erro ao atualizar agendamentos'))
      .finally(() => setRefreshingOrders(false))
  }

  const activeOrders = serviceOrders.filter((order) => order.status !== 'DONE')
  const readyOrders = serviceOrders.filter((order) => order.status === 'READY')

  const logoutAction = (
    <button
      type="button"
      className="icon-btn"
      aria-label="Sair"
      onClick={handleLogout}
    >
      <UiIcon name="logout" size={22} />
    </button>
  )

  const tracking = (
    <TrackingSection
      serviceOrders={serviceOrders}
      vehicles={vehicles}
      refreshing={refreshingOrders}
      statusNotice={statusNotice}
      onRefresh={handleRefreshOrders}
    />
  )

  const home = (
    <ScreenLayout nav={NAV} actions={logoutAction}>
      <div className="greeting">
        <div>
          <small>{greeting()},</small>
          <strong>{firstName(user.name)}</strong>
        </div>
      </div>
      <KpiBand
        items={[
          { label: 'Meus veículos', value: vehicles.length },
          { label: 'Atendimentos ativos', value: activeOrders.length },
          { label: 'Prontos para retirada', value: readyOrders.length },
        ]}
      />
      <Notice tone="error">{loadError}</Notice>
      {readyOrders.length > 0 && (
        <p className="notice notice-success" role="status" aria-live="polite">
          <UiIcon name="check" size={18} />
          Seu veículo está pronto para retirada. Dirija-se ao gerente para
          concluir o atendimento.
        </p>
      )}
      <div className="tile-grid">
        <ActionTile
          to="/cliente/agendar"
          label="Agendar"
          icon="calendar"
          tone="schedule"
        />
        <ActionTile
          to="/cliente/agendamentos"
          label="Acompanhar"
          icon="clock"
          tone="entry"
          hint={
            activeOrders.length ? `${activeOrders.length} ativo(s)` : undefined
          }
        />
      </div>
      {activeOrders.length > 0 && (
        <Link to="/cliente/agendamentos" className="btn btn-outline">
          Ver andamento das lavagens
        </Link>
      )}
    </ScreenLayout>
  )

  return (
    <div className="app">
      <Routes>
        <Route index element={home} />
        <Route
          path="veiculos"
          element={
            <ScreenLayout nav={NAV} actions={logoutAction}>
              <VehicleSection
                vehicles={vehicles}
                customerId={user.id}
                createVehicle={createVehicle}
                onCreated={(vehicle) =>
                  setVehicles((current) => [...current, vehicle])
                }
              />
            </ScreenLayout>
          }
        />
        <Route
          path="agendamentos"
          element={
            <ScreenLayout nav={NAV} actions={logoutAction}>
              <Notice tone="error">{loadError}</Notice>
              {tracking}
            </ScreenLayout>
          }
        />
        <Route
          path="agendar"
          element={
            <ScreenLayout title="Agendar lavagem" backTo="/cliente">
              <ScheduleSection
                vehicles={vehicles}
                servicePrices={servicePrices}
                scheduleServiceOrder={scheduleServiceOrder}
                businessHours={businessHours}
                loadDay={loadDay}
                openOnSunday={openOnSunday}
                onScheduled={(serviceOrder) =>
                  setServiceOrders((current) => [serviceOrder, ...current])
                }
              />
            </ScreenLayout>
          }
        />
        <Route path="*" element={<Navigate to="/cliente" replace />} />
      </Routes>
    </div>
  )
}
