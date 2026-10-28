import { OperationsReportModel } from '@/domain/models/operations-report-model'

const PAUSE_LABELS: Record<
  OperationsReportModel['pauses'][number]['reason'],
  string
> = {
  LUNCH: 'Almoço',
  RESTROOM: 'Banheiro',
  OTHER: 'Outro',
}

const PAYMENT_LABELS: Record<string, string> = {
  CASH: 'Dinheiro',
  PIX: 'Pix',
  DEBIT_CARD: 'Cartão de débito',
  CREDIT_CARD: 'Cartão de crédito',
}

function cell(value: string | number): string {
  const text = String(value)
  const safe = /^[\s]*[=+\-@]/.test(text) ? `'${text}` : text
  return `"${safe.replaceAll('"', '""')}"`
}

export function operationsReportCsv(report: OperationsReportModel): string {
  const rows: (string | number)[][] = [
    ['Relatório Washflow', `${report.from} a ${report.to}`],
    [],
    ['Indicador', 'Valor'],
    ['Atendimentos finalizados', report.finalizedOrders],
    ['Faturamento (R$)', report.revenue.toFixed(2)],
    ['Comissões (R$)', report.commissions.toFixed(2)],
    [
      'Tempo médio de lavagem (min)',
      report.averageWashingMinutes ?? 'Sem dados',
    ],
    [],
    ['Serviço', 'Atendimentos', 'Faturamento (R$)', 'Comissões (R$)'],
    ...report.services.map((service) => [
      service.serviceName,
      service.orders,
      service.revenue.toFixed(2),
      service.commissions.toFixed(2),
    ]),
    [],
    ['Forma de pagamento', 'Atendimentos', 'Valor (R$)'],
    ...report.paymentMethods.map((payment) => [
      PAYMENT_LABELS[payment.method] ?? payment.method,
      payment.orders,
      payment.amount.toFixed(2),
    ]),
    [],
    ['Motivo da pausa', 'Quantidade'],
    ...report.pauses.map((pause) => [PAUSE_LABELS[pause.reason], pause.count]),
  ]

  return rows.map((row) => row.map(cell).join(';')).join('\r\n')
}
