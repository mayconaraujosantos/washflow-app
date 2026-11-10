/** One row of GET /api/audit: a successful change, its author and what was sent (secrets masked). */
export type AuditEntryModel = {
  at: string
  userId: string
  userName: string | null
  profile: string
  method: string
  // Endpoint pattern, e.g. /api/cash-transactions/{id}
  route: string
  path: string
  status: number
  details: string
}
