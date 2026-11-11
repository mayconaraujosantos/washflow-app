import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import type { Access } from '@/domain/usecases/access'
import type { AuthenticateCustomer } from '@/domain/usecases/authenticate-customer'
import type { Establishment } from '@/domain/usecases/establishment'
import type { EstablishmentDirectory } from '@/domain/usecases/platform'
import { BrandLogo } from '@/presentation/components/ui/app-chrome'
import { AccessScreen } from '@/presentation/pages/access/access-screen'

type Props = Readonly<{
  authenticateCustomer: AuthenticateCustomer
  access?: Access
  // Optional so the page still renders where setup status is not wired (tests).
  establishment?: Establishment
  // Lava-jato picker for customers (one installation, many lava-jatos).
  directory?: EstablishmentDirectory
}>

/**
 * Home = the sign-in card of the reference layout ("Entrar" / "Criar uma
 * conta"), with washer and manager access linked under it. A fresh install
 * goes to the setup wizard first.
 */
export function Home({
  authenticateCustomer,
  access,
  establishment,
  directory,
}: Props) {
  const navigate = useNavigate()
  // Until /api/setup answers, a fresh install must not flash the sign-in card
  // (on a cold server that answer can take a while).
  const [checkingSetup, setCheckingSetup] = useState(Boolean(establishment))

  useEffect(() => {
    if (!establishment) return
    let active = true
    void establishment
      .setupStatus()
      .then((status) => {
        if (!active) return
        if (status.configured) setCheckingSetup(false)
        else navigate('/configurar', { replace: true })
      })
      .catch(() => {
        // Offline: still show the card; the request itself will report errors.
        if (active) setCheckingSetup(false)
      })
    return () => {
      active = false
    }
  }, [establishment, navigate])

  if (checkingSetup) {
    return (
      <main className="auth-shell">
        <h1 className="auth-brand">
          <BrandLogo size="lg" />
        </h1>
        <p className="muted" role="status">
          Conectando ao sistema...
        </p>
      </main>
    )
  }

  return (
    <AccessScreen
      authenticateCustomer={authenticateCustomer}
      access={access}
      staffLinks
      directory={directory}
    />
  )
}
