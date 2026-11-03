import { makeRemoteAccess } from '@/main/factories/usecases/make-remote-access'
import { MagicLinkPage } from '@/presentation/pages/access/passwordless'
import { TicketPage } from '@/presentation/pages/ticket/ticket-page'

export const makeTicketPage = () => <TicketPage access={makeRemoteAccess()} />

export const makeMagicLinkPage = () => (
  <MagicLinkPage access={makeRemoteAccess()} />
)
