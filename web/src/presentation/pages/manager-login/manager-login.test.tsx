import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { AuthenticateManager } from '@/domain/usecases/authenticate-manager'
import { UserModel } from '@/domain/models/user-model'
import { ManagerLogin } from './manager-login'

const saveManagerSession = vi.hoisted(() => vi.fn())
vi.mock('@/main/session/manager-session', () => ({ saveManagerSession }))

const user: UserModel = {
  id: '1',
  name: 'Bia',
  phone: '11999993333',
  profile: 'MANAGER',
  createdAt: '2026-01-01T00:00:00Z',
}

const fillAndSubmit = async () => {
  await userEvent.type(screen.getByLabelText('Nome'), user.name)
  await userEvent.type(screen.getByLabelText('Telefone'), user.phone)
  await userEvent.click(screen.getByRole('button', { name: 'Entrar' }))
}

describe('ManagerLogin', () => {
  it('authenticates with the entered name and phone', async () => {
    const authenticateManager: AuthenticateManager = { auth: vi.fn() }
    vi.mocked(authenticateManager.auth).mockResolvedValueOnce(user)
    render(<ManagerLogin authenticateManager={authenticateManager} />)

    await fillAndSubmit()

    expect(authenticateManager.auth).toHaveBeenCalledWith({
      name: user.name,
      phone: user.phone,
    })
  })

  it('saves the session and greets the manager on success', async () => {
    const authenticateManager: AuthenticateManager = { auth: vi.fn() }
    vi.mocked(authenticateManager.auth).mockResolvedValueOnce(user)
    render(<ManagerLogin authenticateManager={authenticateManager} />)

    await fillAndSubmit()

    await waitFor(() => {
      expect(saveManagerSession).toHaveBeenCalledWith(user)
    })
    expect(screen.getByText(/Bia/)).toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: 'Entrar' }),
    ).not.toBeInTheDocument()
  })

  it('shows the not-yet-a-manager message and keeps the form on failure', async () => {
    const authenticateManager: AuthenticateManager = { auth: vi.fn() }
    vi.mocked(authenticateManager.auth).mockRejectedValueOnce(
      new Error('Este telefone ainda não foi promovido a gerente'),
    )
    render(<ManagerLogin authenticateManager={authenticateManager} />)

    await fillAndSubmit()

    expect(
      await screen.findByText(
        'Este telefone ainda não foi promovido a gerente',
      ),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Entrar' })).toBeInTheDocument()
  })
})
