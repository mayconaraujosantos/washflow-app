import { describe, expect, it } from 'vitest'
import { OperationsReportModel } from '@/domain/models/operations-report-model'
import { operationsReportCsv } from './operations-report-csv'

const report: OperationsReportModel = {
  from: '2026-10-01',
  to: '2026-10-26',
  finalizedOrders: 1,
  revenue: 60,
  commissions: 20,
  averageWashingMinutes: 42,
  services: [
    {
      serviceName: '=HYPERLINK("bad")',
      orders: 1,
      revenue: 60,
      commissions: 20,
    },
  ],
  paymentMethods: [{ method: 'PIX', orders: 1, amount: 60 }],
  pauses: [{ reason: 'LUNCH', count: 1 }],
}

describe('operationsReportCsv', () => {
  it('exports all report sections with spreadsheet-safe text cells', () => {
    const csv = operationsReportCsv(report)

    expect(csv).toContain('"Atendimentos finalizados";"1"')
    expect(csv).toContain('"Faturamento (R$)";"60.00"')
    expect(csv).toContain('"\'=HYPERLINK(""bad"")";"1";"60.00";"20.00"')
    expect(csv).toContain('"Pix";"1";"60.00"')
    expect(csv).toContain('"Almoço";"1"')
  })
})
