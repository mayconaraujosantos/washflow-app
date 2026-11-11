import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { Access } from '@/domain/usecases/access'
import { EmailPasswordReset } from './email-password-reset'

const makeAccess = (overrides: Partial<Access> = {}) =>
  ({
    requestPasswordReset: vi.fn().mockResolvedValue(600),
    confirmPasswordReset: vi.fn().mockResolvedValue(undefined),
    ...overrides,
  }) as unknown as Access

describe('EmailPasswordReset', () => {
  it('sends the code by phone and sets the new password with it', async () => {
    const access = makeAccess()
    const onDone = vi.fn()
    render(
      <EmailPasswordReset
        access={access}
        kind="customer"
        onDone={onDone}
        onCancel={vi.fn()}
      />,
    )

    await userEvent.type(screen.getByLabelText('Telefone'), '92991234567')
    await userEvent.click(
      screen.getByRole('button', { name: 'Enviar código por e-mail' }),
    )

    expect(access.requestPasswordReset).toHaveBeenCalledWith({
      kind: 'customer',
      phone: '92991234567',
    })
    expect(
      screen.getByRole('button', { name: /Reenviar código \(60s\)/ }),
    ).toBeDisabled()
    const save = screen.getByRole('button', { name: 'Trocar senha' })
    await userEvent.type(screen.getByLabelText('Código do e-mail'), '12a3456')
    expect(screen.getByLabelText('Código do e-mail')).toHaveValue('123456')
    expect(save).toBeDisabled()
    await userEvent.type(
      screen.getByLabelText('Nova senha (mínimo 8 caracteres)'),
      'senha-nova-456',
    )
    await userEvent.click(save)

    expect(access.confirmPasswordReset).toHaveBeenCalledWith(
      { kind: 'customer', phone: '92991234567' },
      '123456',
      'senha-nova-456',
    )
    expect(onDone).toHaveBeenCalledWith('92991234567', 'senha-nova-456')
  })

  it('asks managers for the login e-mail and shows a wrong code', async () => {
    const access = makeAccess({
      confirmPasswordReset: vi
        .fn()
        .mockRejectedValue(new Error('Código inválido ou expirado')),
    })
    const onDone = vi.fn()
    render(
      <EmailPasswordReset
        access={access}
        kind="manager"
        onDone={onDone}
        onCancel={vi.fn()}
      />,
    )

    await userEvent.type(
      screen.getByLabelText('E-mail de acesso'),
      ' rita@lava.test ',
    )
    await userEvent.click(
      screen.getByRole('button', { name: 'Enviar código por e-mail' }),
    )
    await userEvent.type(screen.getByLabelText('Código do e-mail'), '000000')
    await userEvent.type(
      screen.getByLabelText('Nova senha (mínimo 8 caracteres)'),
      'senha-nova-456',
    )
    await userEvent.click(screen.getByRole('button', { name: 'Trocar senha' }))

    expect(access.requestPasswordReset).toHaveBeenCalledWith({
      kind: 'manager',
      email: 'rita@lava.test',
    })
    expect(
      await screen.findByText('Código inválido ou expirado'),
    ).toBeInTheDocument()
    expect(onDone).not.toHaveBeenCalled()
  })

  it('shows when e-mail is not configured', async () => {
    const access = makeAccess({
      requestPasswordReset: vi
        .fn()
        .mockRejectedValue(
          new Error('Envio de código por e-mail não está configurado'),
        ),
    })
    render(
      <EmailPasswordReset
        access={access}
        kind="customer"
        onDone={vi.fn()}
        onCancel={vi.fn()}
      />,
    )

    await userEvent.type(screen.getByLabelText('Telefone'), '92991234567')
    await userEvent.click(
      screen.getByRole('button', { name: 'Enviar código por e-mail' }),
    )

    expect(
      await screen.findByText(
        'Envio de código por e-mail não está configurado',
      ),
    ).toBeInTheDocument()
    expect(screen.queryByLabelText('Código do e-mail')).not.toBeInTheDocument()
  })
})
