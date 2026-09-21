import { useEffect, useState } from 'react'
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
  const [user] = useState(() => loadCustomerSession())

  const [vehicles, setVehicles] = useState<VehicleModel[]>([])
  const [servicePrices, setServicePrices] = useState<ServicePriceModel[]>([])
  const [serviceOrders, setServiceOrders] = useState<ServiceOrderModel[]>([])
  const [loadError, setLoadError] = useState<string | null>(null)
  const [refreshingOrders, setRefreshingOrders] = useState(false)

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
        setServiceOrders(serviceOrdersResult)
      })
      .catch(() => setLoadError('Erro ao carregar seus dados'))
  }, [
    user,
    listVehiclesByCustomer,
    listServicePrices,
    listServiceOrdersByCustomer,
  ])

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
      .then(setServiceOrders)
      .catch(() => setLoadError('Erro ao atualizar agendamentos'))
      .finally(() => setRefreshingOrders(false))
  }

  return (
    <main className="dashboard-shell">
      <div className="dashboard-container">
        <header className="dashboard-header">
          <div>
            <span className="badge">Washflow · Cliente</span>
            <h1>Olá, {user.name.split(' ')[0]}</h1>
          </div>
          <button className="secondary-btn" onClick={handleLogout}>
            Sair
          </button>
        </header>

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
          onRefresh={handleRefreshOrders}
        />
      </div>
    </main>
  )
}
