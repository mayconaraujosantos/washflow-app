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

const makeLoadHealthStatus = (): LoadHealthStatus => ({
  load: vi.fn().mockResolvedValue({ status: 'ok', service: 'washflow' }),
})

const profiles: Array<[label: string, slug: string]> = [
  ['Cliente', 'cliente'],
  ['Lavador', 'lavador'],
  ['Gerente', 'gerente'],
]

describe('Home', () => {
  it('renders the Washflow heading', () => {
    renderHome(makeLoadHealthStatus())

    expect(
      screen.getByRole('heading', { name: 'Washflow', level: 1 }),
    ).toBeInTheDocument()
  })

  it.each(profiles)(
    'renders a QR code link and a direct login link for %s',
    (label, slug) => {
      renderHome(makeLoadHealthStatus())

      expect(
        screen.getByRole('heading', { name: label, level: 2 }),
      ).toBeInTheDocument()
      expect(
        screen.getByRole('link', { name: `Ver QR Code de ${label}` }),
      ).toHaveAttribute('href', `/qrcodes/${slug}`)
      expect(
        screen.getByRole('link', { name: `Entrar como ${label}` }),
      ).toHaveAttribute('href', `/login/${slug}`)
    },
  )
})
