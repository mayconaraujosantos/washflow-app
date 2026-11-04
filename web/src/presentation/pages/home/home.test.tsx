import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import type { Establishment } from '@/domain/usecases/establishment'
import { LoadHealthStatus } from '@/domain/usecases/load-health-status'
import { Home } from './home'

const renderHome = (loadHealthStatus: LoadHealthStatus) =>
  render(
    <MemoryRouter>
      <Home loadHealthStatus={loadHealthStatus} />
    </MemoryRouter>,
  )

const healthy = (): LoadHealthStatus => ({
  load: vi.fn().mockResolvedValue({ status: 'ok', service: 'washflow' }),
})

const roles: Array<[label: string, slug: string]> = [
  ['Cliente', 'cliente'],
  ['Lavador', 'lavador'],
  ['Gerente', 'gerente'],
]

describe('Home (role cards)', () => {
  it('renders the brand and the slogan', () => {
    renderHome(healthy())

    expect(
      screen.getByRole('heading', { name: 'Washflow', level: 1 }),
    ).toBeInTheDocument()
    expect(
      screen.getByText('Tudo em ordem. Seu veículo, também.'),
    ).toBeInTheDocument()
  })

  it.each(roles)(
    'keeps both actions inside the %s card, login first',
    (label, slug) => {
      renderHome(healthy())

      const card = screen
        .getByRole('heading', { name: label, level: 2 })
        .closest('li') as HTMLElement
      const links = card.querySelectorAll('a')
      expect(links[0]).toHaveAccessibleName(`Entrar como ${label}`)
      expect(links[0]).toHaveAttribute('href', `/login/${slug}`)
      expect(links[0]).toHaveClass('role-btn-primary')
      expect(links[1]).toHaveAccessibleName(`Ver QR Code de ${label}`)
      expect(links[1]).toHaveAttribute('href', `/qrcodes/${slug}`)
      expect(links[1]).toHaveClass('role-btn-secondary')
    },
  )

  it('shows the system as online when the health check passes', async () => {
    renderHome(healthy())

    expect(await screen.findByText('Sistema online')).toBeInTheDocument()
  })

  it('shows the system as offline when the health check fails', async () => {
    renderHome({ load: vi.fn().mockRejectedValue(new Error('down')) })

    expect(await screen.findByText('Sistema offline')).toBeInTheDocument()
  })

  describe('setup check', () => {
    const establishment = (
      setupStatus: Establishment['setupStatus'],
    ): Establishment => ({
      setupStatus,
      setup: vi.fn(),
      load: vi.fn(),
      update: vi.fn(),
    })

    const renderWithSetup = (shop: Establishment) =>
      render(
        <MemoryRouter initialEntries={['/']}>
          <Routes>
            <Route
              path="/"
              element={
                <Home loadHealthStatus={healthy()} establishment={shop} />
              }
            />
            <Route path="/configurar" element={<p>assistente</p>} />
          </Routes>
        </MemoryRouter>,
      )

    it('never shows the login cards on a fresh install', async () => {
      renderWithSetup(
        establishment(
          vi.fn().mockResolvedValue({
            configured: false,
            setupCodeRequired: true,
            establishmentName: 'Meu Lava-Jato',
          }),
        ),
      )

      expect(screen.getByText('Conectando ao sistema...')).toBeInTheDocument()
      expect(
        screen.queryByRole('link', { name: 'Entrar como Gerente' }),
      ).toBeNull()
      expect(await screen.findByText('assistente')).toBeInTheDocument()
    })

    it('shows the cards and the shop name once configured', async () => {
      renderWithSetup(
        establishment(
          vi.fn().mockResolvedValue({
            configured: true,
            setupCodeRequired: true,
            establishmentName: 'Lava Rápido Centro',
          }),
        ),
      )

      expect(await screen.findByText('Lava Rápido Centro')).toBeInTheDocument()
      expect(
        screen.getByRole('link', { name: 'Entrar como Gerente' }),
      ).toBeInTheDocument()
    })

    it('falls back to the cards when the setup check fails', async () => {
      renderWithSetup(establishment(vi.fn().mockRejectedValue(new Error('x'))))

      expect(
        await screen.findByRole('link', { name: 'Entrar como Cliente' }),
      ).toBeInTheDocument()
    })
  })
})
