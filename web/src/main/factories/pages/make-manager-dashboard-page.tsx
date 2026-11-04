import { makeRemoteAccess } from '@/main/factories/usecases/make-remote-access'
import { makeRemoteEstablishment } from '@/main/factories/usecases/make-remote-establishment'
import { makeRemoteFeatures } from '@/main/factories/usecases/make-remote-features'
import { makeRemoteOperations } from '@/main/factories/usecases/make-remote-operations'
import { ManagerApp } from '@/presentation/pages/manager-app/manager-app'

export const makeManagerDashboardPage = () => (
  <ManagerApp
    operations={makeRemoteOperations()}
    features={makeRemoteFeatures()}
    establishment={makeRemoteEstablishment()}
    access={makeRemoteAccess()}
  />
)
