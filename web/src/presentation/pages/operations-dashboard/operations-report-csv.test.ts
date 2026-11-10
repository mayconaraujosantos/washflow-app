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
  refundedOrders: 1,
  refunds: 60,
  reversedCommissions: 20,
  netRevenue: 0,
  netCommissions: 0,
  refundReasons: [{ reason: 'WRONG_CHARGE', count: 1, amount: 60 }],
  cancelledOrders: 2,
  cancellationReasons: [{ reason: 'NO_SHOW', count: 2 }],
  salesReceived: 50,
  saleRefunds: 10,
  netSales: 40,
  saleRefundReasons: [{ reason: 'CUSTOMER_COMPLAINT', count: 1, amount: 10 }],
}

describe('operationsReportCsv', () => {
  it('exports all report sections with spreadsheet-safe text cells', () => {
    const csv = operationsReportCsv(report)

    expect(csv).toContain('"Atendimentos finalizados";"1"')
    expect(csv).toContain('"Faturamento (R$)";"60.00"')
    expect(csv).toContain('"\'=HYPERLINK(""bad"")";"1";"60.00";"20.00"')
    expect(csv).toContain('"Pix";"1";"60.00"')
    expect(csv).toContain('"Almoço";"1"')
    expect(csv).toContain('"Estornos (R$)";"60.00"')
    expect(csv).toContain('"Faturamento líquido (R$)";"0.00"')
    expect(csv).toContain('"Comissões líquidas (R$)";"0.00"')
    expect(csv).toContain('"Cobrança errada";"Atendimento";"1";"60.00"')
    expect(csv).toContain('"Atendimentos cancelados";"2"')
    expect(csv).toContain('"Não compareceu";"2"')
    expect(csv).toContain('"Vendas rápidas recebidas (R$)";"50.00"')
    expect(csv).toContain('"Vendas rápidas líquidas (R$)";"40.00"')
    expect(csv).toContain('"Reclamação do cliente";"Venda rápida";"1";"10.00"')
  })
})
