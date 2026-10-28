import { makeRemoteOperations } from '@/main/factories/usecases/make-remote-operations'
import { OperationsDashboard } from '@/presentation/pages/operations-dashboard/operations-dashboard'

export const makeWasherDashboardPage = () => (
  <OperationsDashboard role="washer" operations={makeRemoteOperations()} />
)
