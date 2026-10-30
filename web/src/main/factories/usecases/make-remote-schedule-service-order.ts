import { RemoteScheduleServiceOrder } from '@/data/usecases/schedule-service-order/remote-schedule-service-order'
import { ScheduleServiceOrder } from '@/domain/usecases/schedule-service-order'
import { FetchHttpClient } from '@/infra/http/fetch-http-client'
import { env } from '@/main/config/env'

export const makeRemoteScheduleServiceOrder = (): ScheduleServiceOrder => {
  return new RemoteScheduleServiceOrder(
    `${env.apiUrl}/api/service-orders`,
    new FetchHttpClient(),
  )
}
