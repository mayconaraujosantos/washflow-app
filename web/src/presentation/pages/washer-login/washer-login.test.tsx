import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { AuthenticateWasher } from '@/domain/usecases/authenticate-washer'
import { UserModel } from '@/domain/models/user-model'
import { WasherLogin } from './washer-login'

const saveWasherSession = vi.hoisted(() => vi.fn())
vi.mock('@/main/session/washer-session', () => ({ saveWasherSession }))

const user: UserModel = {
  id: '1',
  name: 'Ana',
  phone: '11999990000',
  profile: 'WASHER',
  createdAt: '2026-01-01T00:00:00Z',
}

const fillAndSubmit = async () => {
  await userEvent.type(screen.getByLabelText('Nome'), user.name)
  await userEvent.type(screen.getByLabelText('Telefone'), user.phone)
  await userEvent.click(screen.getByRole('button', { name: 'Entrar' }))
}

describe('WasherLogin', () => {
  it('authenticates with the entered name and phone', async () => {
    const authenticateWasher: AuthenticateWasher = { auth: vi.fn() }
    vi.mocked(authenticateWasher.auth).mockResolvedValueOnce(user)
    render(<WasherLogin authenticateWasher={authenticateWasher} />)

    await fillAndSubmit()

    expect(authenticateWasher.auth).toHaveBeenCalledWith({
      name: user.name,
      phone: user.phone,
    })
  })

  it('saves the session and greets the washer on success', async () => {
    const authenticateWasher: AuthenticateWasher = { auth: vi.fn() }
    vi.mocked(authenticateWasher.auth).mockResolvedValueOnce(user)
    render(<WasherLogin authenticateWasher={authenticateWasher} />)

    await fillAndSubmit()

    await waitFor(() => {
      expect(saveWasherSession).toHaveBeenCalledWith(user)
    })
    expect(screen.getByText(/Ana/)).toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: 'Entrar' }),
    ).not.toBeInTheDocument()
  })

  it('shows the error message and keeps the form on failure', async () => {
    const authenticateWasher: AuthenticateWasher = { auth: vi.fn() }
    vi.mocked(authenticateWasher.auth).mockRejectedValueOnce(
      new Error('Telefone já cadastrado com outro perfil'),
    )
    render(<WasherLogin authenticateWasher={authenticateWasher} />)

    await fillAndSubmit()

    expect(
      await screen.findByText('Telefone já cadastrado com outro perfil'),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Entrar' })).toBeInTheDocument()
  })
})
