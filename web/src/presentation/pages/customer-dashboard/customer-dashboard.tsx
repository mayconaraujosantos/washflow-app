import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
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
import { VehicleSection } from './vehicle-section'
import { ScheduleSection } from './schedule-section'
import { TrackingSection } from './tracking-section'
import { UiIcon } from '@/presentation/components/ui-icon'
import './customer-dashboard.css'

type Props = Readonly<{
  createVehicle: CreateVehicle
  listVehiclesByCustomer: ListVehiclesByCustomer
  listServicePrices: ListServicePrices
  scheduleServiceOrder: ScheduleServiceOrder
  listServiceOrdersByCustomer: ListServiceOrdersByCustomer
}>

export function CustomerDashboard({
  createVehicle,
  listVehiclesByCustomer,
  listServicePrices,
  scheduleServiceOrder,
  listServiceOrdersByCustomer,
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
  const previousStatuses = useRef<Map<
    string,
    ServiceOrderModel['status']
  > | null>(null)

  const updateOrders = (
    orders: ServiceOrderModel[],
    announceChanges: boolean,
  ) => {
    const previous = previousStatuses.current
    if (announceChanges && previous) {
      const changedOrder = orders.find(
        (order) =>
          previous.has(order.id) && previous.get(order.id) !== order.status,
      )
      if (changedOrder) {
        const statusLabels: Record<ServiceOrderModel['status'], string> = {
          SCHEDULED: 'Agendado',
          WAITING_IN_YARD: 'Aguardando no pátio',
          WASHING: 'Lavagem iniciada',
          PAUSED: 'Lavagem pausada',
          READY: 'Seu veículo está pronto para retirada',
          DONE: 'Atendimento finalizado',
        }
        setStatusNotice(statusLabels[changedOrder.status])
      }
    }
    previousStatuses.current = new Map(
      orders.map((order) => [order.id, order.status]),
    )
    setServiceOrders(orders)
  }

  useEffect(() => {
    if (!user) {
      navigate('/login/cliente', { replace: true })
    }
  }, [user, navigate])

  useEffect(() => {
    if (!user) return

    Promise.all([
      listVehiclesByCustomer.list({ customerId: user.id }),
      listServicePrices.list(),
      listServiceOrdersByCustomer.list({ customerId: user.id }),
    ])
      .then(([vehiclesResult, servicePricesResult, serviceOrdersResult]) => {
        setVehicles(vehiclesResult)
        setServicePrices(servicePricesResult)
        updateOrders(serviceOrdersResult, false)
      })
      .catch(() => setLoadError('Erro ao carregar seus dados'))
  }, [
    user,
    listVehiclesByCustomer,
    listServicePrices,
    listServiceOrdersByCustomer,
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
    }, 3000)

    return () => {
      active = false
      window.clearInterval(timer)
    }
  }, [user, listServiceOrdersByCustomer])

  if (!user) {
    return null
  }

  const handleLogout = () => {
    clearCustomerSession()
    navigate('/login/cliente')
  }

  const handleRefreshOrders = () => {
    setRefreshingOrders(true)
    listServiceOrdersByCustomer
      .list({ customerId: user.id })
      .then((orders) => updateOrders(orders, true))
      .catch(() => setLoadError('Erro ao atualizar agendamentos'))
      .finally(() => setRefreshingOrders(false))
  }

  return (
    <main className="dashboard-shell">
      <div className="dashboard-container">
        <header className="dashboard-header">
          <div>
            <span className="badge">
              <UiIcon name="car" size={15} />
              Washflow · Cliente
            </span>
            <h1>Olá, {user.name.split(' ')[0]}</h1>
            <p className="dashboard-subtitle">
              Gerencie seus veículos e acompanhe suas lavagens.
            </p>
          </div>
          <button className="secondary-btn" onClick={handleLogout}>
            <UiIcon name="logout" size={16} />
            Sair
          </button>
        </header>

        <div className="dashboard-metrics">
          <article>
            <span className="metric-icon metric-icon-blue">
              <UiIcon name="car" />
            </span>
            <div>
              <span>Meus veículos</span>
              <strong>{vehicles.length}</strong>
            </div>
          </article>
          <article>
            <span className="metric-icon metric-icon-violet">
              <UiIcon name="calendar" />
            </span>
            <div>
              <span>Atendimentos ativos</span>
              <strong>
                {
                  serviceOrders.filter((order) => order.status !== 'DONE')
                    .length
                }
              </strong>
            </div>
          </article>
        </div>

        {loadError && <p className="error-message">{loadError}</p>}

        <VehicleSection
          vehicles={vehicles}
          customerId={user.id}
          createVehicle={createVehicle}
          onCreated={(vehicle) =>
            setVehicles((current) => [...current, vehicle])
          }
        />

        <ScheduleSection
          vehicles={vehicles}
          servicePrices={servicePrices}
          scheduleServiceOrder={scheduleServiceOrder}
          onScheduled={(serviceOrder) =>
            setServiceOrders((current) => [serviceOrder, ...current])
          }
        />

        <TrackingSection
          serviceOrders={serviceOrders}
          vehicles={vehicles}
          refreshing={refreshingOrders}
          statusNotice={statusNotice}
          onRefresh={handleRefreshOrders}
        />
      </div>
    </main>
  )
}
