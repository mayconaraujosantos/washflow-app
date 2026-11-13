/** One active washer on the manager's board (GET /api/operations/washers). */
export type WasherStatusModel = {
  id: string
  name: string
  phone: string | null
  state: 'FREE' | 'WASHING' | 'PAUSED'
  /** The car being washed (or paused); null when free. */
  current: {
    serviceOrderId: string
    vehiclePlate: string
    vehicleModel: string | null
    since: string | null
    pauseReason: string | null
    pausedAt: string | null
  } | null
  /** Waiting cars the manager reserved for this washer. */
  reserved: number
}
