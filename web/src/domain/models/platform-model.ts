export type EstablishmentListingModel = { slug: string; name: string }

export type EstablishmentSummaryModel = {
  id: string
  slug: string
  name: string
  active: boolean
  createdAt: string
  managerName: string | null
  managerEmail: string | null
}
