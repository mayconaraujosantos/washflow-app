import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { UserModel } from '@/domain/models/user-model'
import type { Access } from '@/domain/usecases/access'
import { AccountSection } from './account-section'

const ana: UserModel = {
  id: 'c1',
  name: 'Ana Cliente',
  phone: '92991234567',
  profile: 'CUSTOMER',
  createdAt: '2026-11-10T12:00:00Z',
  active: true,
}

describe('AccountSection', () => {
  it('shows the account and saves the recovery e-mail', async () => {
    const access = {
      loadRecoveryEmail: vi.fn().mockResolvedValue(null),
      saveRecoveryEmail: vi.fn().mockResolvedValue('ana@mail.test'),
    } as unknown as Access
    render(<AccountSection user={ana} access={access} shopName="Lava Raio" />)

    expect(screen.getByText('Ana Cliente')).toBeInTheDocument()
    expect(screen.getByText('Lava Raio')).toBeInTheDocument()
    const save = screen.getByRole('button', { name: 'Salvar e-mail' })
    await userEvent.type(
      await screen.findByLabelText('E-mail para recuperar a senha'),
      'Ana@Mail.test',
    )
    await userEvent.click(save)

    expect(access.saveRecoveryEmail).toHaveBeenCalledWith('c1', 'Ana@Mail.test')
    expect(
      await screen.findByText(
        'Pronto: se esquecer a senha, enviamos um código para ana@mail.test.',
      ),
    ).toBeInTheDocument()
    expect(save).toBeDisabled()
  })

  it('starts from the saved e-mail', async () => {
    const access = {
      loadRecoveryEmail: vi.fn().mockResolvedValue('ana@mail.test'),
      saveRecoveryEmail: vi.fn(),
    } as unknown as Access
    render(<AccountSection user={ana} access={access} shopName="" />)

    expect(await screen.findByDisplayValue('ana@mail.test')).toBeInTheDocument()
  })
})
