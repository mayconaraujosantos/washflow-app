/**
 * The lava-jato a customer picked on the home screen (or reached through the
 * shop's link/QR: `/?lavajato=<address>`). Sent as `X-Establishment` on every
 * request so public screens (catalog, schedule, sign-in) know which shop they
 * are for. Staff never need it: their account says which lava-jato it is.
 */
export type EstablishmentChoice = { slug: string; name: string }

const STORAGE_KEY = 'washflow:establishment'
const SLUG = /^[a-z0-9-]{2,60}$/

export function loadEstablishmentChoice(): EstablishmentChoice | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw) as Partial<EstablishmentChoice>
    if (typeof parsed.slug !== 'string' || !SLUG.test(parsed.slug)) return null
    return {
      slug: parsed.slug,
      name: typeof parsed.name === 'string' ? parsed.name : '',
    }
  } catch {
    return null
  }
}

export function saveEstablishmentChoice(choice: EstablishmentChoice): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(choice))
  } catch {
    // Private mode: the choice lasts for this page only.
  }
}

export function clearEstablishmentChoice(): void {
  try {
    localStorage.removeItem(STORAGE_KEY)
  } catch {
    // nothing stored
  }
}

/**
 * Adopts `?lavajato=<address>` from the shop's link or QR code and removes it
 * from the address bar. The name is filled in once the picker confirms it.
 */
export function adoptEstablishmentFromUrl(
  location: Location = window.location,
): void {
  const params = new URLSearchParams(location.search)
  const slug = params.get('lavajato')?.trim().toLowerCase()
  if (!slug || !SLUG.test(slug)) return
  const current = loadEstablishmentChoice()
  if (current?.slug !== slug) saveEstablishmentChoice({ slug, name: '' })
  params.delete('lavajato')
  const query = params.toString()
  const search = query ? '?' + query : ''
  window.history.replaceState(
    window.history.state,
    '',
    location.pathname + search + location.hash,
  )
}
