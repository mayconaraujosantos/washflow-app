import type { AuthenticatedSessionModel } from '@/domain/models/authenticated-session-model'
import type {
  EstablishmentModel,
  ScheduleDayModel,
  SetupStatusModel,
} from '@/domain/models/establishment-model'

export type EstablishmentParams = {
  name: string
  openingHour: number
  closingHour: number
  openOnSunday: boolean
  maxVehiclesPerHour: number
}

export type SetupParams = Omit<EstablishmentParams, 'name'> & {
  setupCode?: string
  establishmentName: string
  ownerName: string
  ownerEmail: string
  ownerPhone?: string
  ownerPassword: string
}

export interface Establishment {
  setupStatus: () => Promise<SetupStatusModel>
  /** Creates the establishment and the owner manager; resolves with the owner's session. */
  setup: (params: SetupParams) => Promise<AuthenticatedSessionModel>
  load: () => Promise<EstablishmentModel>
  update: (params: EstablishmentParams) => Promise<EstablishmentModel>
  /** Bookable hours of a 'YYYY-MM-DD' shop-local date, with remaining capacity. */
  availability: (date: string) => Promise<ScheduleDayModel>
}
