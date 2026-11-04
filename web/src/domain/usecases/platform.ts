import type { AuthenticatedSessionModel } from '@/domain/models/authenticated-session-model'
import type {
  EstablishmentListingModel,
  EstablishmentSummaryModel,
} from '@/domain/models/platform-model'

export type NewEstablishmentParams = {
  name: string
  slug?: string
  openingHour: number
  closingHour: number
  openOnSunday: boolean
  maxVehiclesPerHour: number
  managerName: string
  managerEmail: string
  managerPhone?: string
  managerPassword: string
}

/** Public lava-jato directory (customer picker). */
export interface EstablishmentDirectory {
  search: (query: string) => Promise<EstablishmentListingModel[]>
}

/** Platform administrator: creates and suspends lava-jatos. */
export interface Platform extends EstablishmentDirectory {
  signIn: (
    email: string,
    password: string,
  ) => Promise<AuthenticatedSessionModel>
  list: () => Promise<EstablishmentSummaryModel[]>
  create: (params: NewEstablishmentParams) => Promise<EstablishmentSummaryModel>
  setActive: (id: string, active: boolean) => Promise<EstablishmentSummaryModel>
}
