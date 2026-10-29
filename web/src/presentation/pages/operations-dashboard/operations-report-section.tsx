import { useState } from 'react'
import { UiIcon } from '@/presentation/components/ui-icon'
import type { OperationsReportModel } from '@/domain/models/operations-report-model'
import type { Operations } from '@/domain/usecases/operations'
import { operationsReportCsv } from './operations-report-csv'

const PAYMENT_LABELS: Record<string, string> = {
  CASH: 'Dinheiro',
  PIX: 'Pix',
  DEBIT_CARD: 'Cartão de débito',
  CREDIT_CARD: 'Cartão de crédito',
}

const PAUSE_LABELS: Record<string, string> = {
  LUNCH: 'Almoço',
  RESTROOM: 'Banheiro',
  OTHER: 'Outro',
}

function currentManausDate(): string {
  const parts = Object.fromEntries(
    new Intl.DateTimeFormat('en', {
      timeZone: 'America/Manaus',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    })
      .formatToParts(new Date())
      .map(({ type, value }) => [type, value]),
  )
  return `${parts.year}-${parts.month}-${parts.day}`
}

function downloadReport(report: OperationsReportModel): void {
  const blob = new Blob([`\uFEFF${operationsReportCsv(report)}`], {
    type: 'text/csv;charset=utf-8',
  })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `washflow-relatorio-${report.from}-${report.to}.csv`
  link.click()
  URL.revokeObjectURL(url)
}

type Props = Readonly<{
  operations: Operations
}>

export function OperationsReportSection({ operations }: Props) {
  const [report, setReport] = useState<OperationsReportModel | null>(null)
  const [reportLoading, setReportLoading] = useState(false)
  const [reportError, setReportError] = useState<string | null>(null)
  const [reportFrom, setReportFrom] = useState(() => {
    const today = currentManausDate()
    return `${today.slice(0, 7)}-01`
  })
  const [reportTo, setReportTo] = useState(currentManausDate)

  const generateReport = async () => {
    setReportLoading(true)
    setReportError(null)
    setReport(null)
    try {
      setReport(await operations.loadReport(reportFrom, reportTo))
    } catch (cause) {
      setReportError(
        cause instanceof Error ? cause.message : 'Falha ao carregar o relatório',
      )
    } finally {
      setReportLoading(false)
    }
  }

  return (
    <section className="dashboard-section report-section">
      <div className="section-header">
        <div>
          <h2 className="section-title">
            <span className="section-icon">
              <UiIcon name="chart" />
            </span>
            Relatório operacional
          </h2>
          <p>Atendimentos finalizados no período, horário de Manaus.</p>
        </div>
        <div className="report-actions">
          <button
            type="button"
            className="primary-btn"
            disabled={reportLoading}
            onClick={() => void generateReport()}
          >
            <UiIcon name="chart" size={16} />
            {reportLoading ? 'Gerando...' : 'Gerar relatório'}
          </button>
          <button
            type="button"
            className="secondary-btn"
            disabled={!report || reportLoading}
            onClick={() => report && downloadReport(report)}
          >
            <UiIcon name="download" size={16} />
            Exportar CSV
          </button>
        </div>
      </div>
      <div className="report-filters">
        <label>
          De
          <input
            type="date"
            value={reportFrom}
            max={reportTo}
            onChange={(event) => {
              setReport(null)
              setReportFrom(event.target.value)
            }}
          />
        </label>
        <label>
          Até
          <input
            type="date"
            value={reportTo}
            min={reportFrom}
            onChange={(event) => {
              setReport(null)
              setReportTo(event.target.value)
            }}
          />
        </label>
      </div>
      {reportError && (
        <p className="error-message" role="alert">
          {reportError}
        </p>
      )}
      {report && (
        <>
          <div className="report-metrics">
            <article>
              <span>Atendimentos finalizados</span>
              <strong>{report.finalizedOrders}</strong>
            </article>
            <article>
              <span>Faturamento</span>
              <strong>
                {report.revenue.toLocaleString('pt-BR', {
                  style: 'currency',
                  currency: 'BRL',
                })}
              </strong>
            </article>
            <article>
              <span>Comissões de lavadores</span>
              <strong>
                {report.commissions.toLocaleString('pt-BR', {
                  style: 'currency',
                  currency: 'BRL',
                })}
              </strong>
            </article>
            <article>
              <span>Tempo médio de lavagem</span>
              <strong>
                {report.averageWashingMinutes === null
                  ? 'Sem dados'
                  : `${report.averageWashingMinutes} min`}
              </strong>
            </article>
          </div>
          <div className="report-tables">
            <div>
              <h3>Por serviço</h3>
              {report.services.length === 0 ? (
                <p className="empty-state">Sem atendimentos no período.</p>
              ) : (
                <table>
                  <thead>
                    <tr>
                      <th>Serviço</th>
                      <th>Qtd.</th>
                      <th>Faturamento</th>
                      <th>Comissões</th>
                    </tr>
                  </thead>
                  <tbody>
                    {report.services.map((service) => (
                      <tr key={service.serviceName}>
                        <td>{service.serviceName}</td>
                        <td>{service.orders}</td>
                        <td>
                          {service.revenue.toLocaleString('pt-BR', {
                            style: 'currency',
                            currency: 'BRL',
                          })}
                        </td>
                        <td>
                          {service.commissions.toLocaleString('pt-BR', {
                            style: 'currency',
                            currency: 'BRL',
                          })}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
            <div>
              <h3>Por forma de pagamento</h3>
              {report.paymentMethods.length === 0 ? (
                <p className="empty-state">Sem pagamentos no período.</p>
              ) : (
                <table>
                  <thead>
                    <tr>
                      <th>Forma</th>
                      <th>Qtd.</th>
                      <th>Valor</th>
                    </tr>
                  </thead>
                  <tbody>
                    {report.paymentMethods.map((payment) => (
                      <tr key={payment.method}>
                        <td>{PAYMENT_LABELS[payment.method] ?? payment.method}</td>
                        <td>{payment.orders}</td>
                        <td>
                          {payment.amount.toLocaleString('pt-BR', {
                            style: 'currency',
                            currency: 'BRL',
                          })}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
            <div>
              <h3>Pausas registradas</h3>
              {report.pauses.length === 0 ? (
                <p className="empty-state">Nenhuma pausa no período.</p>
              ) : (
                <ul className="report-pause-list">
                  {report.pauses.map((pause) => (
                    <li key={pause.reason}>
                      <span>{PAUSE_LABELS[pause.reason]}</span>
                      <strong>{pause.count}</strong>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>
        </>
      )}
      {!report && !reportLoading && !reportError && (
        <p className="empty-state">Selecione o período e gere o relatório.</p>
      )}
    </section>
  )
}
