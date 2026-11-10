import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { manausDate } from '@/main/config/business-time'
import { CashScreen } from './back-office-screens'
import { ManagerContext, type ManagerData } from './manager-context'

vi.mock('@/presentation/components/cash-manager', () => ({
  CashManager: () => null,
}))

describe('CashScreen', () => {
  it("subtracts today's order and sale refunds from the day balance", async () => {
    const listSaleRefunds = vi
      .fn()
      .mockResolvedValue([{ amount: 15 }, { amount: 5 }])
    const operations = {
      loadReport: vi.fn().mockResolvedValue({ revenue: 300, refunds: 100 }),
      listSales: vi.fn().mockResolvedValue([{ paidAmount: 70 }]),
      listSaleRefunds,
    }
    const features = {
      transactionReport: vi
        .fn()
        .mockResolvedValue({ totalIncome: 50, totalExpense: 30 }),
    }
    render(
      <ManagerContext.Provider
        value={{ operations, features } as unknown as ManagerData}
      >
        <MemoryRouter>
          <CashScreen />
        </MemoryRouter>
      </ManagerContext.Provider>,
    )

    const refunds = await screen.findByText('Estornos')
    expect(refunds.nextElementSibling).toHaveTextContent(/R\$\s*120,00/)
    // 300 + 70 + 50 - 30 - 120
    expect(
      screen.getByText('Saldo do dia').nextElementSibling,
    ).toHaveTextContent(/R\$\s*270,00/)
    expect(listSaleRefunds).toHaveBeenCalledWith(manausDate(), manausDate())
  })
})
