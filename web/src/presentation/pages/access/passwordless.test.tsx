import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { NeedsNameError, type Access } from '@/domain/usecases/access'
import { TicketPage } from '@/presentation/pages/ticket/ticket-page'
import { badgeTokenFrom } from '@/presentation/pages/washer-login/badge-token'
import { WasherLogin } from '@/presentation/pages/washer-login/washer-login'
import { CodeLogin, MagicLinkPage } from './passwordless'

const saveCustomerSession = vi.hoisted(() => vi.fn())
const saveWasherSession = vi.hoisted(() => vi.fn())
vi.mock('@/main/session/customer-session', () => ({ saveCustomerSession }))
vi.mock('@/main/session/washer-session', () => ({ saveWasherSession }))

const session = {
  token: 't',
  user: {
    id: '1',
    name: 'Ana',
    phone: '92991234567',
    profile: 'CUSTOMER' as const,
    createdAt: '2026-11-03T00:00:00Z',
    active: true,
  },
}

const makeAccess = (overrides: Partial<Access> = {}): Access => ({
  issueWasherBadge: vi.fn(),
  washerPinLogin: vi.fn().mockResolvedValue(session),
  washerBadgeLogin: vi.fn().mockResolvedValue(session),
  issueTicket: vi.fn(),
  loadTicket: vi.fn(),
  options: vi.fn().mockResolvedValue({
    whatsappCode: true,
    emailCode: true,
    googleClientId: null,
  }),
  requestCode: vi.fn().mockResolvedValue(600),
  verifyCode: vi.fn().mockResolvedValue(session),
  googleLogin: vi.fn(),
  requestPasswordReset: vi.fn(),
  confirmPasswordReset: vi.fn(),
  loadRecoveryEmail: vi.fn(),
  saveRecoveryEmail: vi.fn(),
  ...overrides,
})

describe('CodeLogin', () => {
  it('sends the code, asks the name for a new phone and signs up', async () => {
    const access = makeAccess({
      verifyCode: vi
        .fn()
        .mockRejectedValueOnce(new NeedsNameError())
        .mockResolvedValueOnce(session),
    })
    const onSession = vi.fn()
    render(
      <CodeLogin
        access={access}
        channel="whatsapp"
        onSession={onSession}
        onCancel={vi.fn()}
      />,
    )

    await userEvent.type(screen.getByLabelText('Seu WhatsApp'), '92991234567')
    await userEvent.click(screen.getByRole('button', { name: 'Enviar código' }))
    expect(access.requestCode).toHaveBeenCalledWith({ phone: '92991234567' })
    expect(await screen.findByText(/vale por 10 minutos/)).toBeInTheDocument()

    await userEvent.type(screen.getByLabelText('Código'), '123456')
    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }))
    await userEvent.type(await screen.findByLabelText('Seu nome'), 'Ana')
    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(access.verifyCode).toHaveBeenLastCalledWith(
      { phone: '92991234567' },
      '123456',
      'Ana',
    )
    expect(saveCustomerSession).toHaveBeenCalledWith(session)
    expect(onSession).toHaveBeenCalledWith(session, true)
  })

  it('sends the code to an e-mail and signs in', async () => {
    const access = makeAccess()
    const onSession = vi.fn()
    render(
      <CodeLogin
        access={access}
        channel="email"
        onSession={onSession}
        onCancel={vi.fn()}
      />,
    )

    await userEvent.type(screen.getByLabelText('Seu e-mail'), ' ana@email.com ')
    await userEvent.click(screen.getByRole('button', { name: 'Enviar código' }))
    expect(access.requestCode).toHaveBeenCalledWith({ email: 'ana@email.com' })
    expect(
      await screen.findByText(/código para o seu e-mail/),
    ).toBeInTheDocument()

    await userEvent.type(screen.getByLabelText('Código'), '123456')
    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(access.verifyCode).toHaveBeenLastCalledWith(
      { email: 'ana@email.com' },
      '123456',
      undefined,
    )
    expect(onSession).toHaveBeenCalledWith(session, false)
  })
})

describe('MagicLinkPage', () => {
  it('verifies the code from the link and opens the customer panel', async () => {
    const access = makeAccess()
    render(
      <MemoryRouter
        initialEntries={['/entrar/whatsapp?telefone=92991234567&codigo=654321']}
      >
        <Routes>
          <Route
            path="/entrar/whatsapp"
            element={<MagicLinkPage access={access} />}
          />
          <Route path="/cliente" element={<p>painel do cliente</p>} />
        </Routes>
      </MemoryRouter>,
    )

    expect(await screen.findByText('painel do cliente')).toBeInTheDocument()
    expect(access.verifyCode).toHaveBeenCalledWith(
      { phone: '92991234567' },
      '654321',
      undefined,
    )
  })

  it('verifies the code from the e-mail link', async () => {
    const access = makeAccess()
    render(
      <MemoryRouter
        initialEntries={['/entrar/email?email=ana%40email.com&codigo=654321']}
      >
        <Routes>
          <Route
            path="/entrar/email"
            element={<MagicLinkPage access={access} />}
          />
          <Route path="/cliente" element={<p>painel do cliente</p>} />
        </Routes>
      </MemoryRouter>,
    )

    expect(await screen.findByText('painel do cliente')).toBeInTheDocument()
    expect(access.verifyCode).toHaveBeenCalledWith(
      { email: 'ana@email.com' },
      '654321',
      undefined,
    )
  })
})

describe('WasherLogin with badge', () => {
  const renderLogin = (access: Access, url = '/login/lavador') =>
    render(
      <MemoryRouter initialEntries={[url]}>
        <Routes>
          <Route
            path="/login/lavador"
            element={
              <WasherLogin
                authenticateWasher={{ auth: vi.fn() }}
                access={access}
              />
            }
          />
          <Route path="/lavador" element={<p>painel do lavador</p>} />
        </Routes>
      </MemoryRouter>,
    )

  it('signs in with phone and 6-digit PIN', async () => {
    const access = makeAccess()
    renderLogin(access)

    await userEvent.type(screen.getByLabelText('Telefone'), '92991234567')
    await userEvent.type(screen.getByLabelText('PIN'), '12ab3456')
    await userEvent.click(
      screen.getByRole('button', { name: 'Entrar com PIN' }),
    )

    expect(access.washerPinLogin).toHaveBeenCalledWith('92991234567', '123456')
    expect(await screen.findByText('painel do lavador')).toBeInTheDocument()
  })

  it('signs in straight from the badge link', async () => {
    const access = makeAccess()
    renderLogin(
      access,
      '/login/lavador?cracha=abcdefghijklmnopqrstuvwxyz012345',
    )

    expect(await screen.findByText('painel do lavador')).toBeInTheDocument()
    expect(access.washerBadgeLogin).toHaveBeenCalledWith(
      'abcdefghijklmnopqrstuvwxyz012345',
    )
  })

  it('reads the badge token from the scanned QR value', () => {
    expect(
      badgeTokenFrom(
        'https://lava.app/login/lavador?cracha=abc_DEF-123456789012345',
      ),
    ).toBe('abc_DEF-123456789012345')
    expect(badgeTokenFrom('abcdefghijklmnopqrstuvwx')).toBe(
      'abcdefghijklmnopqrstuvwx',
    )
    expect(badgeTokenFrom('not a token')).toBeNull()
  })
})

describe('TicketPage', () => {
  it('shows the progress of the order without logging in', async () => {
    const access = makeAccess({
      loadTicket: vi.fn().mockResolvedValue({
        establishmentName: 'Lava Rápido Centro',
        vehiclePlate: 'QWE2R34',
        vehicleModel: 'HB20',
        serviceName: 'Lavagem Completa',
        status: 'READY',
        pauseReason: null,
        scheduledAt: '2026-11-03T14:00:00Z',
        expectedDeliveryAt: null,
        updatedAt: '2026-11-03T15:00:00Z',
      }),
    })
    render(
      <MemoryRouter initialEntries={['/ticket/token-123456789012']}>
        <Routes>
          <Route
            path="/ticket/:token"
            element={<TicketPage access={access} />}
          />
        </Routes>
      </MemoryRouter>,
    )

    expect(await screen.findByText('Pronto para retirada!')).toBeInTheDocument()
    expect(screen.getByText('QWE2R34')).toBeInTheDocument()
    expect(access.loadTicket).toHaveBeenCalledWith('token-123456789012')
    expect(screen.getByText('Pronto').closest('li')).toHaveAttribute(
      'aria-current',
      'step',
    )
  })
})
