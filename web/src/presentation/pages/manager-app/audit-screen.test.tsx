import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import type { AuditEntryModel } from '@/domain/models/audit-entry-model'
import { manausDate } from '@/main/config/business-time'
import { AuditScreen } from './audit-screen'
import { ManagerContext, type ManagerData } from './manager-context'

const entry: AuditEntryModel = {
  at: '2026-11-10T15:30:00Z',
  userId: 'u-1',
  userName: 'Ana Souza',
  profile: 'MANAGER',
  method: 'DELETE',
  route: '/api/cash-transactions/{id}',
  path: '/api/cash-transactions/abc',
  status: 204,
  details: '',
}

const renderScreen = (loadAudit: ReturnType<typeof vi.fn>) =>
  render(
    <ManagerContext.Provider
      value={{ operations: { loadAudit } } as unknown as ManagerData}
    >
      <MemoryRouter>
        <AuditScreen />
      </MemoryRouter>
    </ManagerContext.Provider>,
  )

describe('AuditScreen', () => {
  it("shows today's changes with who did them", async () => {
    const loadAudit = vi.fn().mockResolvedValue([entry])

    renderScreen(loadAudit)

    expect(
      await screen.findByText('Excluiu um lançamento do caixa'),
    ).toBeInTheDocument()
    expect(screen.getByText(/Ana Souza/)).toBeInTheDocument()
    expect(loadAudit).toHaveBeenCalledWith(manausDate(), manausDate())
  })

  it('searches another period', async () => {
    const loadAudit = vi.fn().mockResolvedValue([])
    renderScreen(loadAudit)
    expect(
      await screen.findByText('Nenhuma alteração neste período.'),
    ).toBeInTheDocument()

    const from = screen.getByLabelText('De')
    await userEvent.clear(from)
    await userEvent.type(from, '2026-11-01')
    await userEvent.click(screen.getByRole('button', { name: 'Buscar' }))

    expect(loadAudit).toHaveBeenLastCalledWith('2026-11-01', manausDate())
  })

  it('shows the API error', async () => {
    const loadAudit = vi.fn().mockRejectedValue(new Error('Período inválido'))

    renderScreen(loadAudit)

    expect(await screen.findByText('Período inválido')).toBeInTheDocument()
  })
})
