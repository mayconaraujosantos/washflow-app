import { makeRemoteFeatures } from '@/main/factories/usecases/make-remote-features'
import { makeRemoteOperations } from '@/main/factories/usecases/make-remote-operations'
import { OperationsDashboard } from '@/presentation/pages/operations-dashboard/operations-dashboard'

export const makeManagerDashboardPage = () => (
  <OperationsDashboard
    role="manager"
    operations={makeRemoteOperations()}
    features={makeRemoteFeatures()}
  />
)
