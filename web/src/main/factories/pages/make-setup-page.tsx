import { makeRemoteEstablishment } from '@/main/factories/usecases/make-remote-establishment'
import { SetupWizard } from '@/presentation/pages/setup/setup-wizard'

export const makeSetupPage = () => (
  <SetupWizard establishment={makeRemoteEstablishment()} />
)
