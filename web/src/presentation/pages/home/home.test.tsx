import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import type { AuthenticateCustomer } from '@/domain/usecases/authenticate-customer'
import type { Establishment } from '@/domain/usecases/establishment'
import { Home } from './home'

const authenticateCustomer: AuthenticateCustomer = { auth: vi.fn() }

const establishment = (
  setupStatus: Establishment['setupStatus'],
): Establishment => ({
  setupStatus,
  setup: vi.fn(),
  load: vi.fn(),
  update: vi.fn(),
  availability: vi.fn(),
  whatsappStatus: vi.fn(),
  connectWhatsapp: vi.fn(),
  disconnectWhatsapp: vi.fn(),
})

const renderHome = (shop?: Establishment) =>
  render(
    <MemoryRouter initialEntries={['/']}>
      <Routes>
        <Route
          path="/"
          element={
            <Home
              authenticateCustomer={authenticateCustomer}
              establishment={shop}
            />
          }
        />
        <Route path="/configurar" element={<p>assistente</p>} />
      </Routes>
    </MemoryRouter>,
  )

const configured = () =>
  establishment(
    vi.fn().mockResolvedValue({
      configured: true,
      setupCodeRequired: true,
      establishmentName: 'Lava Rápido Centro',
    }),
  )

describe('Home (sign-in card of the reference layout)', () => {
  it('opens on "Entrar" with the brand, phone and password', async () => {
    renderHome(configured())

    expect(
      await screen.findByRole('heading', { name: 'Entrar' }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('heading', { name: 'Washflow', level: 1 }),
    ).toBeInTheDocument()
    expect(screen.getByLabelText('Telefone')).toBeInTheDocument()
    expect(screen.getByLabelText('Senha')).toBeInTheDocument()
  })

  it('switches to "Criar uma conta" from the card links', async () => {
    renderHome(configured())

    await userEvent.click(
      await screen.findByRole('button', { name: 'Criar conta' }),
    )

    expect(
      screen.getByRole('heading', { name: 'Criar uma conta' }),
    ).toBeInTheDocument()
    expect(screen.getByLabelText('Nome')).toBeInTheDocument()
  })

  it('links washers and managers to their own logins', async () => {
    renderHome(configured())

    expect(
      await screen.findByRole('link', { name: 'Entrar como lavador' }),
    ).toHaveAttribute('href', '/login/lavador')
    expect(
      screen.getByRole('link', { name: 'Entrar como gerente' }),
    ).toHaveAttribute('href', '/login/gerente')
  })

  it('goes to the setup wizard on a fresh install without flashing the card', async () => {
    renderHome(
      establishment(
        vi.fn().mockResolvedValue({
          configured: false,
          setupCodeRequired: true,
          establishmentName: 'Meu Lava-Jato',
        }),
      ),
    )

    expect(screen.getByText('Conectando ao sistema...')).toBeInTheDocument()
    expect(screen.queryByLabelText('Telefone')).toBeNull()
    expect(await screen.findByText('assistente')).toBeInTheDocument()
  })

  it('still shows the card when the setup check fails', async () => {
    renderHome(establishment(vi.fn().mockRejectedValue(new Error('offline'))))

    expect(
      await screen.findByRole('heading', { name: 'Entrar' }),
    ).toBeInTheDocument()
  })
})
