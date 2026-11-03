import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import type { AuthenticatedSessionModel } from '@/domain/models/authenticated-session-model'
import type { AuthenticateCustomer } from '@/domain/usecases/authenticate-customer'
import { AccessScreen } from './access-screen'

const saveCustomerSession = vi.hoisted(() => vi.fn())
vi.mock('@/main/session/customer-session', () => ({ saveCustomerSession }))

const session = (
  profile: 'CUSTOMER' | 'MANAGER',
): AuthenticatedSessionModel => ({
  token: 'signed-token',
  user: {
    id: '1',
    name: 'Bia',
    phone: '92991234567',
    profile,
    createdAt: '2026-01-01T00:00:00Z',
    active: true,
  },
})

const setup = () => {
  const authenticateCustomer: AuthenticateCustomer = {
    auth: vi.fn().mockResolvedValue(session('CUSTOMER')),
  }
  render(
    <MemoryRouter initialEntries={['/login/cliente']}>
      <Routes>
        <Route
          path="/login/cliente"
          element={<AccessScreen authenticateCustomer={authenticateCustomer} />}
        />
        <Route path="/cliente" element={<p>painel do cliente</p>} />
      </Routes>
    </MemoryRouter>,
  )
  return { authenticateCustomer }
}

describe('AccessScreen', () => {
  it('shows the Washflow brand and the create-account form first', () => {
    setup()

    expect(
      screen.getByRole('heading', { name: 'Washflow', level: 1 }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('heading', { name: 'Criar uma conta' }),
    ).toBeInTheDocument()
    expect(screen.getByLabelText('Nome')).toBeInTheDocument()
    expect(screen.getByText('+55')).toBeInTheDocument()
  })

  it('creates a customer account with the phone digits and shows the first steps', async () => {
    const { authenticateCustomer } = setup()

    await userEvent.type(screen.getByLabelText('Nome'), 'Bia Souza')
    await userEvent.type(screen.getByLabelText('Telefone'), '92991234567')
    await userEvent.type(
      screen.getByLabelText('Senha (mínimo 8 caracteres)'),
      'senha-forte',
    )
    expect(screen.getByLabelText('Telefone')).toHaveValue('(92) 99123-4567')
    await userEvent.click(
      screen.getByRole('button', { name: 'Criar uma conta' }),
    )

    expect(authenticateCustomer.auth).toHaveBeenCalledWith({
      name: 'Bia Souza',
      phone: '92991234567',
      password: 'senha-forte',
    })
    expect(saveCustomerSession).toHaveBeenCalled()
    expect(
      await screen.findByRole('heading', { name: 'Bem-vindo ao Washflow!' }),
    ).toBeInTheDocument()
    await userEvent.click(
      screen.getByRole('button', { name: 'Perfeito, entendi, vou começar' }),
    )
    expect(screen.getByText('painel do cliente')).toBeInTheDocument()
  })

  it('signs a returning customer in with phone and password only', async () => {
    const { authenticateCustomer } = setup()

    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }))
    expect(screen.queryByLabelText('Nome')).not.toBeInTheDocument()
    await userEvent.type(screen.getByLabelText('Telefone'), '92991234567')
    await userEvent.type(screen.getByLabelText('Senha'), 'senha-forte')
    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(authenticateCustomer.auth).toHaveBeenCalledWith({
      phone: '92991234567',
      password: 'senha-forte',
    })
    expect(await screen.findByText('painel do cliente')).toBeInTheDocument()
  })

  it('reveals the password on demand', async () => {
    setup()
    const password = screen.getByLabelText('Senha (mínimo 8 caracteres)')

    expect(password).toHaveAttribute('type', 'password')
    await userEvent.click(screen.getByRole('button', { name: 'Mostrar senha' }))
    expect(password).toHaveAttribute('type', 'text')
  })
})
