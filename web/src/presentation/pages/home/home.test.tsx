import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
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
})
