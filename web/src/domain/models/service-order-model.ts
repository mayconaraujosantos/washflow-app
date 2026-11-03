// Mirrors ServiceOrderStatus's Java-side names (com.washflow.domain.entities.ServiceOrderStatus)
// - Jackson serializes the enum by name, not by its Portuguese dbValue().
export type ServiceOrderStatus =
  'SCHEDULED' | 'WAITING_IN_YARD' | 'WASHING' | 'PAUSED' | 'READY' | 'DONE'

// Display data joined by the API (ServiceOrder.Details) - absent on older responses.
export type ServiceOrderDetails = {
  vehiclePlate: string | null
  vehicleModel: string | null
  vehicleColor: string | null
  customerName: string | null
  customerPhone: string | null
  serviceName: string | null
  washerName: string | null
}

export type ServiceOrderModel = {
  id: string
  vehicleId: string
  servicePriceId: number
  washerId: string | null
  status: ServiceOrderStatus
  scheduledAt: string
  createdAt: string
  updatedAt: string
  pauseReason?: 'LUNCH' | 'RESTROOM' | 'OTHER' | null
  expectedDeliveryAt?: string | null
  notes?: string | null
  details?: ServiceOrderDetails | null
}
