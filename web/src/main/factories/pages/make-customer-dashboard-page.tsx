import { CustomerDashboard } from '@/presentation/pages/customer-dashboard/customer-dashboard'
import { makeRemoteCreateVehicle } from '@/main/factories/usecases/make-remote-create-vehicle'
import { makeRemoteListVehiclesByCustomer } from '@/main/factories/usecases/make-remote-list-vehicles-by-customer'
import { makeRemoteListServicePrices } from '@/main/factories/usecases/make-remote-list-service-prices'
import { makeRemoteScheduleServiceOrder } from '@/main/factories/usecases/make-remote-schedule-service-order'
import { makeRemoteListServiceOrdersByCustomer } from '@/main/factories/usecases/make-remote-list-service-orders-by-customer'

export const makeCustomerDashboardPage = () => {
  return (
    <CustomerDashboard
      createVehicle={makeRemoteCreateVehicle()}
      listVehiclesByCustomer={makeRemoteListVehiclesByCustomer()}
      listServicePrices={makeRemoteListServicePrices()}
      scheduleServiceOrder={makeRemoteScheduleServiceOrder()}
      listServiceOrdersByCustomer={makeRemoteListServiceOrdersByCustomer()}
    />
  )
}
