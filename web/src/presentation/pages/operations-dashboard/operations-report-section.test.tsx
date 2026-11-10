import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { OperationsReportModel } from '@/domain/models/operations-report-model'
import type { Operations } from '@/domain/usecases/operations'
import { OperationsReportSection } from './operations-report-section'

const report: OperationsReportModel = {
  from: '2026-11-01',
  to: '2026-11-10',
  finalizedOrders: 3,
  revenue: 300,
  commissions: 60,
  averageWashingMinutes: 40,
  services: [],
  paymentMethods: [],
  pauses: [],
  refundedOrders: 1,
  refunds: 100,
  reversedCommissions: 0,
  netRevenue: 200,
  netCommissions: 60,
  refundReasons: [{ reason: 'SERVICE_NOT_DONE', count: 1, amount: 100 }],
  cancelledOrders: 3,
  cancellationReasons: [
    { reason: 'NO_SHOW', count: 2 },
    { reason: 'REGISTERED_BY_MISTAKE', count: 1 },
  ],
  salesReceived: 80,
  saleRefunds: 30,
  netSales: 50,
  saleRefundReasons: [{ reason: 'WRONG_CHARGE', count: 1, amount: 30 }],
}

describe('OperationsReportSection', () => {
  it('shows cancellations, refunds by reason and quick sales', async () => {
    const operations = {
      loadReport: vi.fn().mockResolvedValue(report),
    } as unknown as Operations
    render(<OperationsReportSection operations={operations} />)

    await userEvent.click(screen.getByRole('button', { name: /Gerar/ }))

    const cancellations = await screen.findByRole('region', {
      name: 'Cancelamentos (3)',
    })
    expect(
      within(cancellations).getByText('Não compareceu'),
    ).toBeInTheDocument()
    expect(within(cancellations).getByText('2')).toBeInTheDocument()
    expect(
      within(cancellations).getByText('Lançado por engano'),
    ).toBeInTheDocument()

    const refunds = screen.getByRole('region', { name: 'Estornos por motivo' })
    expect(
      within(refunds).getByText('Serviço não realizado'),
    ).toBeInTheDocument()
    expect(within(refunds).getByText('Atendimento')).toBeInTheDocument()
    expect(within(refunds).getByText('Cobrança errada')).toBeInTheDocument()
    expect(within(refunds).getByText('Venda rápida')).toBeInTheDocument()

    expect(screen.getByText('Vendas rápidas líquidas')).toBeInTheDocument()
    expect(
      screen.getByText('Vendas rápidas líquidas').nextElementSibling,
    ).toHaveTextContent(/R\$\s*50,00/)
  })
})
