// GET /api/operations/vehicles - a vehicle plus its registered owner or walk-in contact.
export type VehicleLookupModel = {
  vehicleId: string
  plate: string
  model: string | null
  color: string | null
  customerId: string | null
  customerName: string | null
  customerPhone: string | null
}
