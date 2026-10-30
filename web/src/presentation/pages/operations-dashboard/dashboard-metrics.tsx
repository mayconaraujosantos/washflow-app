import { UiIcon } from '@/presentation/components/ui-icon'

type WasherMetrics = Readonly<{
  type: 'washer'
  waitingCount: number
  myOrdersCount: number
  pausedCount: number
}>

type ManagerMetrics = Readonly<{
  type: 'manager'
  yardCount: number
  washingCount: number
  readyCount: number
}>

type Props = Readonly<{
  metrics: WasherMetrics | ManagerMetrics
}>

export function DashboardMetrics({ metrics }: Props) {
  if (metrics.type === 'washer') {
    return (
      <div className="dashboard-metrics">
        <article>
          <span className="metric-icon metric-icon-blue">
            <UiIcon name="clipboard" />
          </span>
          <div>
            <span>Na fila</span>
            <strong>{metrics.waitingCount}</strong>
          </div>
        </article>
        <article>
          <span className="metric-icon metric-icon-violet">
            <UiIcon name="washer" />
          </span>
          <div>
            <span>Minhas lavagens</span>
            <strong>{metrics.myOrdersCount}</strong>
          </div>
        </article>
        <article>
          <span className="metric-icon metric-icon-amber">
            <UiIcon name="pause" />
          </span>
          <div>
            <span>Pausadas</span>
            <strong>{metrics.pausedCount}</strong>
          </div>
        </article>
      </div>
    )
  }

  return (
    <div className="dashboard-metrics">
      <article>
        <span className="metric-icon metric-icon-blue">
          <UiIcon name="clipboard" />
        </span>
        <div>
          <span>No pátio</span>
          <strong>{metrics.yardCount}</strong>
        </div>
      </article>
      <article>
        <span className="metric-icon metric-icon-violet">
          <UiIcon name="washer" />
        </span>
        <div>
          <span>Em lavagem</span>
          <strong>{metrics.washingCount}</strong>
        </div>
      </article>
      <article>
        <span className="metric-icon metric-icon-green">
          <UiIcon name="check" />
        </span>
        <div>
          <span>Aguardando retirada</span>
          <strong>{metrics.readyCount}</strong>
        </div>
      </article>
    </div>
  )
}
