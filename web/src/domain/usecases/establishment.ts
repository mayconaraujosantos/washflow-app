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

/** First run: the platform administrator, who then creates each lava-jato. */
export type SetupParams = {
  setupCode?: string
  ownerName: string
  ownerEmail: string
  ownerPassword: string
}

export interface Establishment {
  setupStatus: () => Promise<SetupStatusModel>
  /** Creates the platform administrator; resolves with the admin session. */
  setup: (params: SetupParams) => Promise<AuthenticatedSessionModel>
  load: () => Promise<EstablishmentModel>
  update: (params: EstablishmentParams) => Promise<EstablishmentModel>
  /** Bookable hours of a 'YYYY-MM-DD' shop-local date, with remaining capacity. */
  availability: (date: string) => Promise<ScheduleDayModel>
}
