import { makeRemoteOperations } from '@/main/factories/usecases/make-remote-operations'
import { WasherApp } from '@/presentation/pages/washer-app/washer-app'

export const makeWasherDashboardPage = () => (
  <WasherApp operations={makeRemoteOperations()} />
)
