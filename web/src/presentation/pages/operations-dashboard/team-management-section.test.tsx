import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import type { UserModel } from '@/domain/models/user-model'
import type { Operations } from '@/domain/usecases/operations'
import { TeamManagementSection } from './team-management-section'

const account = (
  id: string,
  name: string,
  profile: UserModel['profile'],
): UserModel =>
  ({
    id,
    name,
    phone: '92991234567',
    profile,
    active: true,
    createdAt: '2026-11-10T12:00:00Z',
  }) as UserModel

const manager = account('m1', 'Rita Gerente', 'MANAGER')
const washer = account('w1', 'Lucas Lavador', 'WASHER')
const customer = account('c1', 'Ana Cliente', 'CUSTOMER')

const renderTeam = (operations: Partial<Operations>) => {
  const setNotice = vi.fn()
  const setError = vi.fn()
  render(
    <MemoryRouter>
      <TeamManagementSection
        users={[manager, washer, customer]}
        currentUser={manager}
        operations={operations as Operations}
        teamLoading={false}
        teamError={null}
        busyUserId={null}
        setBusyUserId={vi.fn()}
        setNotice={setNotice}
        setError={setError}
      />
    </MemoryRouter>,
  )
  return { setNotice, setError }
}

describe('TeamManagementSection password reset', () => {
  it('offers it for customers and washers, never for managers', () => {
    renderTeam({})

    expect(
      screen.getByRole('button', { name: 'Redefinir senha de Ana Cliente' }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: 'Redefinir senha de Lucas Lavador' }),
    ).toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: 'Redefinir senha de Rita Gerente' }),
    ).not.toBeInTheDocument()
  })

  it('saves the new password and tells the manager to hand it over', async () => {
    const resetUserPassword = vi.fn().mockResolvedValue(customer)
    const { setNotice } = renderTeam({ resetUserPassword })

    await userEvent.click(
      screen.getByRole('button', { name: 'Redefinir senha de Ana Cliente' }),
    )
    await userEvent.type(
      screen.getByLabelText('Nova senha de Ana Cliente'),
      'nova-senha-123',
    )
    await userEvent.click(
      screen.getByRole('button', { name: 'Salvar nova senha' }),
    )

    expect(resetUserPassword).toHaveBeenCalledWith('c1', 'nova-senha-123')
    expect(setNotice).toHaveBeenLastCalledWith(
      'Senha de Ana Cliente redefinida. Entregue a nova senha pessoalmente.',
    )
    expect(
      screen.queryByLabelText('Nova senha de Ana Cliente'),
    ).not.toBeInTheDocument()
  })

  it('keeps the form open and shows the API error', async () => {
    const resetUserPassword = vi
      .fn()
      .mockRejectedValue(
        new Error('A nova senha deve ter no mínimo 8 caracteres'),
      )
    const { setError } = renderTeam({ resetUserPassword })

    await userEvent.click(
      screen.getByRole('button', { name: 'Redefinir senha de Lucas Lavador' }),
    )
    await userEvent.type(
      screen.getByLabelText('Nova senha de Lucas Lavador'),
      'nova-senha-123',
    )
    await userEvent.click(
      screen.getByRole('button', { name: 'Salvar nova senha' }),
    )

    expect(setError).toHaveBeenLastCalledWith(
      'A nova senha deve ter no mínimo 8 caracteres',
    )
    expect(
      screen.getByLabelText('Nova senha de Lucas Lavador'),
    ).toBeInTheDocument()
  })
})
