import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import type { AuthenticatedSessionModel } from '@/domain/models/authenticated-session-model'
import type { AuthenticateCustomer } from '@/domain/usecases/authenticate-customer'
import type { AuthenticateManager } from '@/domain/usecases/authenticate-manager'
import type { AuthenticateWasher } from '@/domain/usecases/authenticate-washer'
import { Home } from './home'

const saveCustomerSession = vi.hoisted(() => vi.fn())
const saveManagerSession = vi.hoisted(() => vi.fn())
vi.mock('@/main/session/customer-session', () => ({ saveCustomerSession }))
vi.mock('@/main/session/manager-session', () => ({ saveManagerSession }))
vi.mock('@/main/session/washer-session', () => ({ saveWasherSession: vi.fn() }))

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

const setup = (staff = true) => {
  const authenticateCustomer: AuthenticateCustomer = {
    auth: vi.fn().mockResolvedValue(session('CUSTOMER')),
  }
  const authenticateManager: AuthenticateManager = {
    auth: vi.fn().mockResolvedValue(session('MANAGER')),
  }
  const authenticateWasher: AuthenticateWasher = { auth: vi.fn() }
  render(
    <MemoryRouter initialEntries={['/']}>
      <Routes>
        <Route
          path="/"
          element={
            <Home
              authenticateCustomer={authenticateCustomer}
              authenticateWasher={staff ? authenticateWasher : undefined}
              authenticateManager={staff ? authenticateManager : undefined}
            />
          }
        />
        <Route path="/gerente" element={<p>painel do gerente</p>} />
        <Route path="/cliente" element={<p>painel do cliente</p>} />
      </Routes>
    </MemoryRouter>,
  )
  return { authenticateCustomer, authenticateManager }
}

describe('Home (access screen)', () => {
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

  it('signs a manager in with phone and password and opens the panel', async () => {
    const { authenticateManager, authenticateCustomer } = setup()

    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }))
    await userEvent.click(screen.getByRole('button', { name: 'Gerente' }))
    await userEvent.type(screen.getByLabelText('Telefone'), '92991234567')
    await userEvent.type(screen.getByLabelText('Senha'), 'senha-forte')
    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(authenticateManager.auth).toHaveBeenCalledWith({
      phone: '92991234567',
      password: 'senha-forte',
    })
    expect(authenticateCustomer.auth).not.toHaveBeenCalled()
    expect(saveManagerSession).toHaveBeenCalled()
    expect(await screen.findByText('painel do gerente')).toBeInTheDocument()
  })

  it('reveals the password on demand', async () => {
    setup()
    const password = screen.getByLabelText('Senha (mínimo 8 caracteres)')

    expect(password).toHaveAttribute('type', 'password')
    await userEvent.click(screen.getByRole('button', { name: 'Mostrar senha' }))
    expect(password).toHaveAttribute('type', 'text')
  })

  it('offers only the customer profile on the customer QR code page', async () => {
    setup(false)

    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(
      screen.queryByRole('group', { name: 'Entrar como' }),
    ).not.toBeInTheDocument()
  })
})
