import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import type { Establishment } from '@/domain/usecases/establishment'
import { SetupWizard } from './setup-wizard'

const saveManagerSession = vi.hoisted(() => vi.fn())
vi.mock('@/main/session/manager-session', () => ({ saveManagerSession }))

const session = {
  token: 't',
  user: {
    id: '1',
    name: 'Dona Maria',
    phone: '',
    profile: 'MANAGER' as const,
    createdAt: '2026-11-03T00:00:00Z',
    active: true,
  },
}

const makeEstablishment = (configured = false, setupCodeRequired = true) =>
  ({
    setupStatus: vi.fn().mockResolvedValue({
      configured,
      setupCodeRequired,
      establishmentName: 'Meu Lava-Jato',
    }),
    setup: vi.fn().mockResolvedValue(session),
    load: vi.fn(),
    update: vi.fn(),
    availability: vi.fn(),
  }) satisfies Establishment

const renderWizard = (establishment: Establishment) =>
  render(
    <MemoryRouter initialEntries={['/configurar']}>
      <Routes>
        <Route
          path="/configurar"
          element={<SetupWizard establishment={establishment} />}
        />
        <Route path="/gerente" element={<p>painel do gerente</p>} />
        <Route path="/" element={<p>tela inicial</p>} />
      </Routes>
    </MemoryRouter>,
  )

describe('SetupWizard', () => {
  it('collects the car wash and the owner account, then opens the panel', async () => {
    const establishment = makeEstablishment()
    renderWizard(establishment)

    await userEvent.type(
      await screen.findByLabelText('Nome do lava-jato'),
      'Lava Rápido Centro',
    )
    await userEvent.click(screen.getByLabelText('Abre aos domingos'))
    await userEvent.click(screen.getByRole('button', { name: 'Continuar' }))

    await userEvent.type(screen.getByLabelText('Seu nome'), 'Dona Maria')
    await userEvent.type(
      screen.getByLabelText('E-mail de acesso'),
      'maria@lava.com',
    )
    await userEvent.type(
      screen.getByLabelText('Senha (mínimo 8 caracteres)'),
      'senha-forte',
    )
    await userEvent.type(
      screen.getByLabelText('Código de configuração'),
      's3gredo',
    )
    await userEvent.click(
      screen.getByRole('button', { name: 'Concluir e abrir o painel' }),
    )

    expect(establishment.setup).toHaveBeenCalledWith({
      setupCode: 's3gredo',
      establishmentName: 'Lava Rápido Centro',
      openingHour: 8,
      closingHour: 18,
      openOnSunday: true,
      maxVehiclesPerHour: 3,
      ownerName: 'Dona Maria',
      ownerEmail: 'maria@lava.com',
      ownerPhone: undefined,
      ownerPassword: 'senha-forte',
    })
    expect(saveManagerSession).toHaveBeenCalledWith(session)
    expect(await screen.findByText('painel do gerente')).toBeInTheDocument()
  })

  it('does not ask for the setup code when the server does not require it', async () => {
    renderWizard(makeEstablishment(false, false))

    await userEvent.type(
      await screen.findByLabelText('Nome do lava-jato'),
      'Lava',
    )
    await userEvent.click(screen.getByRole('button', { name: 'Continuar' }))

    expect(
      screen.queryByLabelText('Código de configuração'),
    ).not.toBeInTheDocument()
  })

  it('leaves the wizard when the establishment is already configured', async () => {
    renderWizard(makeEstablishment(true))

    expect(await screen.findByText('tela inicial')).toBeInTheDocument()
  })
})
