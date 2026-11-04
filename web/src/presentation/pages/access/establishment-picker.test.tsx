import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { AuthenticateCustomer } from '@/domain/usecases/authenticate-customer'
import type { EstablishmentDirectory } from '@/domain/usecases/platform'
import { AccessScreen } from './access-screen'

const authenticateCustomer: AuthenticateCustomer = { auth: vi.fn() }

const directory = (): EstablishmentDirectory => ({
  search: vi.fn().mockResolvedValue([
    { slug: 'lava-centro', name: 'Lava Centro' },
    { slug: 'lava-bairro', name: 'Lava Bairro' },
  ]),
})

const renderScreen = (dir: EstablishmentDirectory) =>
  render(
    <MemoryRouter>
      <AccessScreen
        authenticateCustomer={authenticateCustomer}
        initialMode="signin"
        staffLinks
        directory={dir}
      />
    </MemoryRouter>,
  )

describe('AccessScreen with the lava-jato picker', () => {
  beforeEach(() => localStorage.clear())

  it('asks the customer to pick a lava-jato first, keeping staff links', async () => {
    renderScreen(directory())

    expect(
      screen.getByRole('heading', { name: 'Qual é o seu lava-jato?' }),
    ).toBeInTheDocument()
    expect(screen.queryByLabelText('Senha')).toBeNull()
    expect(
      screen.getByRole('link', { name: 'Entrar como lavador' }),
    ).toBeInTheDocument()

    await userEvent.click(
      await screen.findByRole('button', { name: 'Lava Centro' }),
    )

    expect(screen.getByRole('heading', { name: 'Entrar' })).toBeInTheDocument()
    expect(screen.getByText('Lava Centro')).toBeInTheDocument()
    expect(localStorage.getItem('washflow:establishment')).toContain(
      'lava-centro',
    )
  })

  it('lets the customer change the lava-jato', async () => {
    localStorage.setItem(
      'washflow:establishment',
      JSON.stringify({ slug: 'lava-centro', name: 'Lava Centro' }),
    )
    renderScreen(directory())

    await userEvent.click(screen.getByRole('button', { name: 'Trocar' }))

    expect(
      screen.getByRole('heading', { name: 'Qual é o seu lava-jato?' }),
    ).toBeInTheDocument()
    expect(localStorage.getItem('washflow:establishment')).toBeNull()
  })

  it('confirms the name of a lava-jato that came from a shop link', async () => {
    localStorage.setItem(
      'washflow:establishment',
      JSON.stringify({ slug: 'lava-bairro', name: '' }),
    )
    renderScreen(directory())

    expect(await screen.findByText('Lava Bairro')).toBeInTheDocument()
  })
})
