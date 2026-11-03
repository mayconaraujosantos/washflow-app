import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { manausDate } from '@/main/config/business-time'
import { UiIcon } from '@/presentation/components/ui-icon'
import { ScreenLayout } from '@/presentation/components/ui/app-chrome'
import { ActionTile, KpiBand } from '@/presentation/components/ui/blocks'
import { firstName, greeting } from '@/presentation/helpers/format'
import { IN_YARD_STATUSES, MANAGER_NAV, useManager } from './manager-context'

const KPI_REFRESH_MS = 30000

export function ManagerHome() {
  const { user, operations, yard, openMenu } = useManager()
  const [scheduledToday, setScheduledToday] = useState<number | null>(null)
  const [doneToday, setDoneToday] = useState<number | null>(null)

  useEffect(() => {
    let active = true
    const load = async () => {
      const today = manausDate()
      const [agenda, report] = await Promise.allSettled([
        operations.loadAgenda(today, today),
        operations.loadReport(today, today),
      ])
      if (!active) return
      if (agenda.status === 'fulfilled') {
        setScheduledToday(
          agenda.value.filter((order) => order.status === 'SCHEDULED').length,
        )
      }
      if (report.status === 'fulfilled') {
        setDoneToday(report.value.finalizedOrders)
      }
    }
    void load()
    const timer = window.setInterval(() => void load(), KPI_REFRESH_MS)
    return () => {
      active = false
      window.clearInterval(timer)
    }
  }, [operations])

  const inYard = yard.filter((order) =>
    IN_YARD_STATUSES.includes(order.status),
  ).length
  const ready = yard.filter((order) => order.status === 'READY').length

  return (
    <ScreenLayout onMenu={openMenu} nav={MANAGER_NAV}>
      <div className="greeting">
        <div>
          <small>{greeting()},</small>
          <strong>{firstName(user.name)}</strong>
        </div>
        <Link to="/gerente/caixa" className="cash-shortcut">
          <UiIcon name="money" size={26} />
          Caixa do dia
        </Link>
      </div>

      <KpiBand
        items={[
          { label: 'Agendados para hoje', value: scheduledToday ?? '–' },
          { label: 'Veículos no pátio', value: inYard },
          { label: 'Concluídos hoje', value: doneToday ?? '–' },
        ]}
      />

      <div className="quick-row">
        <Link to="/gerente/venda" className="quick-sale-btn">
          <UiIcon name="bolt" size={20} />
          Venda rápida
        </Link>
        <Link to="/gerente/caixa" className="cash-shortcut">
          <UiIcon name="cart" size={26} />
          Despesa
        </Link>
      </div>

      <div className="tile-grid">
        <ActionTile
          to="/gerente/entrada"
          label="Entrada"
          icon="entry"
          tone="entry"
        />
        <ActionTile
          to="/gerente/saida"
          label="Saída"
          icon="exit"
          tone="exit"
          hint={ready ? `${ready} pronto${ready > 1 ? 's' : ''}` : undefined}
        />
        <ActionTile
          to="/gerente/agendar"
          label="Agendar"
          icon="calendar"
          tone="schedule"
        />
        <ActionTile
          to="/gerente/orcamento"
          label="Orçamento"
          icon="list"
          tone="budget"
        />
      </div>
    </ScreenLayout>
  )
}
