import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { Establishment } from '@/domain/usecases/establishment'
import { SetupWizard } from './setup-wizard'

const session = {
  token: 'admin-token',
  user: {
    id: 'a1',
    name: 'Dono',
    phone: null,
    profile: 'ADMIN',
    createdAt: '2026-11-04T12:00:00Z',
  },
}

const establishment = (configured = false, setupCodeRequired = true) =>
  ({
    setupStatus: vi.fn().mockResolvedValue({
      configured,
      setupCodeRequired,
      establishmentName: null,
    }),
    setup: vi.fn().mockResolvedValue(session),
    load: vi.fn(),
    update: vi.fn(),
    availability: vi.fn(),
  }) as unknown as Establishment & { setup: ReturnType<typeof vi.fn> }

const renderWizard = (shop: Establishment) =>
  render(
    <MemoryRouter initialEntries={['/configurar']}>
      <Routes>
        <Route
          path="/configurar"
          element={<SetupWizard establishment={shop} />}
        />
        <Route path="/admin" element={<p>painel admin</p>} />
        <Route path="/" element={<p>inicio</p>} />
      </Routes>
    </MemoryRouter>,
  )

describe('SetupWizard (platform administrator)', () => {
  beforeEach(() => localStorage.clear())

  it('creates the administrator with the setup code and opens the admin panel', async () => {
    const shop = establishment()
    renderWizard(shop)

    await userEvent.type(await screen.findByLabelText('Seu nome'), 'Dono')
    await userEvent.type(
      screen.getByLabelText('E-mail de acesso'),
      'dono@washflow.com',
    )
    await userEvent.type(
      screen.getByLabelText('Senha (mínimo 8 caracteres)'),
      'senha-forte',
    )
    await userEvent.type(screen.getByLabelText('Código de configuração'), 'abc')
    await userEvent.click(
      screen.getByRole('button', { name: 'Criar administrador' }),
    )

    expect(await screen.findByText('painel admin')).toBeInTheDocument()
    expect(shop.setup).toHaveBeenCalledWith({
      setupCode: 'abc',
      ownerName: 'Dono',
      ownerEmail: 'dono@washflow.com',
      ownerPassword: 'senha-forte',
    })
    expect(localStorage.getItem('washflow:admin-session')).toContain(
      'admin-token',
    )
  })

  it('leaves for the home page when the platform is already configured', async () => {
    renderWizard(establishment(true))

    expect(await screen.findByText('inicio')).toBeInTheDocument()
  })
})
