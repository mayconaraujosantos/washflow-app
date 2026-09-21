// Mirrors ServiceOrderStatus's Java-side names (com.washflow.domain.entities.ServiceOrderStatus)
// - Jackson serializes the enum by name, not by its Portuguese dbValue().
export type ServiceOrderStatus =
  'SCHEDULED' | 'WAITING_IN_YARD' | 'WASHING' | 'READY' | 'DONE'

export type ServiceOrderModel = {
  id: string
  vehicleId: string
  servicePriceId: number
  washerId: string | null
  status: ServiceOrderStatus
  scheduledAt: string
  createdAt: string
  updatedAt: string
}
