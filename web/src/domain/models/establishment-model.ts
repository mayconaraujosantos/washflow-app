// GET /api/establishment - the single car wash this installation serves.
export type EstablishmentModel = {
  name: string
  openingHour: number
  closingHour: number
  openOnSunday: boolean
  maxVehiclesPerHour: number
  configuredAt: string | null
}

// GET /api/setup - whether the first-run wizard is still pending.
export type SetupStatusModel = {
  configured: boolean
  setupCodeRequired: boolean
  establishmentName: string
}
