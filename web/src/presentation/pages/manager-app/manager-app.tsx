import { useCallback, useEffect, useMemo, useState } from 'react'
import { Navigate, Route, Routes, useNavigate } from 'react-router-dom'
import type { ServiceOrderModel } from '@/domain/models/service-order-model'
import type { ServicePriceModel } from '@/domain/models/service-price-model'
import type { EstablishmentModel } from '@/domain/models/establishment-model'
import type { Access } from '@/domain/usecases/access'
import type { Establishment } from '@/domain/usecases/establishment'
import type { Features } from '@/domain/usecases/features'
import type { Operations } from '@/domain/usecases/operations'
import {
  clearManagerSession,
  loadManagerSession,
} from '@/main/session/manager-session'
import { SideMenu } from '@/presentation/components/ui/app-chrome'
import { errorMessage, firstName } from '@/presentation/helpers/format'
import { AgendaScreen } from './agenda-screen'
import { CashScreen, ReportScreen, TeamScreen } from './back-office-screens'
import { EntryScreen } from './entry-screen'
import { ManagerHome } from './manager-home'
import {
  ManagerContext,
  type FlashTone,
  type ManagerData,
} from './manager-context'
import { SaleScreen } from './sale-screen'
import { SettingsScreen } from './settings-screen'
import { YardScreen } from './yard-screen'

type Props = Readonly<{
  operations: Operations
  features: Features
  establishment: Establishment
  access: Access
}>

const REFRESH_MS = 3000
const FLASH_MS = 4500

export function ManagerApp({
  operations,
  features,
  establishment,
  access,
}: Props) {
  const navigate = useNavigate()
  const [session] = useState(() => loadManagerSession())
  const user = session?.user ?? null

  const [yard, setYard] = useState<ServiceOrderModel[]>([])
  const [servicePrices, setServicePrices] = useState<ServicePriceModel[]>([])
  const [flash, setFlash] = useState<{ tone: FlashTone; text: string } | null>(
    null,
  )
  const [menuOpen, setMenuOpen] = useState(false)
  const [settings, setSettings] = useState<EstablishmentModel | null>(null)

  const notify = useCallback((tone: FlashTone, text: string) => {
    setFlash({ tone, text })
  }, [])

  const refresh = useCallback(async () => {
    try {
      setYard(await operations.loadYard())
    } catch (cause) {
      notify('error', errorMessage(cause, 'Falha ao atualizar o pátio'))
    }
  }, [operations, notify])

  const reloadPrices = useCallback(async () => {
    try {
      setServicePrices(await operations.loadServicePrices())
    } catch (cause) {
      notify('error', errorMessage(cause, 'Falha ao carregar o catálogo'))
    }
  }, [operations, notify])

  const reloadSettings = useCallback(async () => {
    try {
      setSettings(await establishment.load())
    } catch (cause) {
      notify(
        'error',
        errorMessage(cause, 'Falha ao carregar o estabelecimento'),
      )
    }
  }, [establishment, notify])

  useEffect(() => {
    if (!user) {
      navigate('/login/gerente', { replace: true })
      return
    }
    const first = window.setTimeout(() => {
      void refresh()
      void reloadPrices()
      void reloadSettings()
    }, 0)
    const timer = window.setInterval(() => void refresh(), REFRESH_MS)
    return () => {
      window.clearTimeout(first)
      window.clearInterval(timer)
    }
  }, [user, navigate, refresh, reloadPrices, reloadSettings])

  useEffect(() => {
    if (!flash) return
    const timer = window.setTimeout(() => setFlash(null), FLASH_MS)
    return () => window.clearTimeout(timer)
  }, [flash])

  const openMenu = useCallback(() => setMenuOpen(true), [])
  const closeMenu = useCallback(() => setMenuOpen(false), [])

  const data = useMemo<ManagerData | null>(
    () =>
      user && {
        user,
        operations,
        features,
        yard,
        servicePrices,
        refresh,
        reloadPrices,
        notify,
        openMenu,
        establishment,
        settings,
        reloadSettings,
        access,
      },
    [
      user,
      operations,
      features,
      yard,
      servicePrices,
      refresh,
      reloadPrices,
      notify,
      openMenu,
      establishment,
      settings,
      reloadSettings,
      access,
    ],
  )

  if (!data) return null

  const logout = () => {
    clearManagerSession()
    navigate('/login/gerente')
  }

  return (
    <ManagerContext.Provider value={data}>
      <div className="app">
        <Routes>
          <Route index element={<ManagerHome />} />
          <Route path="patio" element={<YardScreen mode="yard" />} />
          <Route path="saida" element={<YardScreen mode="exit" />} />
          <Route path="agenda" element={<AgendaScreen />} />
          <Route path="entrada" element={<EntryScreen mode="entry" />} />
          <Route path="agendar" element={<EntryScreen mode="schedule" />} />
          <Route path="venda" element={<SaleScreen type="QUICK_SALE" />} />
          <Route path="orcamento" element={<SaleScreen type="BUDGET" />} />
          <Route path="caixa" element={<CashScreen />} />
          <Route path="equipe" element={<TeamScreen />} />
          <Route path="relatorios" element={<ReportScreen />} />
          <Route path="estabelecimento" element={<SettingsScreen />} />
          <Route path="*" element={<Navigate to="/gerente" replace />} />
        </Routes>
        {flash && (
          <div
            className={`toast notice notice-${flash.tone}`}
            role={flash.tone === 'error' ? 'alert' : 'status'}
          >
            {flash.text}
          </div>
        )}
        <SideMenu
          open={menuOpen}
          onClose={closeMenu}
          title={`Olá, ${firstName(data.user.name)}`}
          subtitle="Gerente"
          items={[
            { label: 'Início', icon: 'home', to: '/gerente' },
            { label: 'Caixa e despesas', icon: 'money', to: '/gerente/caixa' },
            { label: 'Relatórios', icon: 'chart', to: '/gerente/relatorios' },
            {
              label: 'Equipe e acessos',
              icon: 'manager',
              to: '/gerente/equipe',
            },
            {
              label: 'Estabelecimento',
              icon: 'tag',
              to: '/gerente/estabelecimento',
            },
          ]}
          footer={[{ label: 'Sair', icon: 'logout', onClick: logout }]}
        />
      </div>
    </ManagerContext.Provider>
  )
}
