import { describe, expect, it, vi } from 'vitest'
import { NeedsNameError } from '@/domain/usecases/access'
import { RemoteAccess } from './remote-access'

const make = (response: { statusCode: number; body?: unknown }) => {
  const request = vi.fn().mockResolvedValue(response)
  return { request, sut: new RemoteAccess('https://w.test', { request }) }
}

describe('RemoteAccess', () => {
  it('logs a washer in with phone and PIN', async () => {
    const { request, sut } = make({ statusCode: 200, body: { token: 't' } })

    await sut.washerPinLogin('92991234567', '123456')

    expect(request).toHaveBeenCalledWith({
      url: 'https://w.test/api/auth/washers/pin',
      method: 'post',
      body: { phone: '92991234567', pin: '123456' },
    })
  })

  it('returns the ticket token', async () => {
    const { sut } = make({ statusCode: 200, body: { token: 'abc' } })

    await expect(sut.issueTicket('order-1')).resolves.toBe('abc')
  })

  it('sends the code request and verification to the e-mail', async () => {
    const { request, sut } = make({ statusCode: 200, body: { token: 't' } })

    await sut.verifyCode({ email: 'ana@email.com' }, '123456', 'Ana')

    expect(request).toHaveBeenCalledWith({
      url: 'https://w.test/api/auth/customers/otp/verify',
      method: 'post',
      body: { email: 'ana@email.com', code: '123456', name: 'Ana' },
    })
  })

  it('asks for the name when the code is right but the phone is new', async () => {
    const { sut } = make({
      statusCode: 400,
      body: { error: 'Informe seu nome para criar a conta', needsName: true },
    })

    await expect(
      sut.verifyCode({ phone: '92991234567' }, '123456'),
    ).rejects.toBeInstanceOf(NeedsNameError)
  })

  it('asks for a reset code by phone (customer) or e-mail (manager)', async () => {
    const { request, sut } = make({
      statusCode: 202,
      body: { expiresInSeconds: 600 },
    })

    await expect(
      sut.requestPasswordReset({ kind: 'customer', phone: '92991234567' }),
    ).resolves.toBe(600)
    await sut.requestPasswordReset({ kind: 'manager', email: 'rita@lava.test' })

    expect(request).toHaveBeenNthCalledWith(1, {
      url: 'https://w.test/api/auth/customers/password-reset',
      method: 'post',
      body: { phone: '92991234567' },
    })
    expect(request).toHaveBeenNthCalledWith(2, {
      url: 'https://w.test/api/auth/managers/password-reset',
      method: 'post',
      body: { email: 'rita@lava.test' },
    })
  })

  it('confirms the new password with the code', async () => {
    const { request, sut } = make({
      statusCode: 200,
      body: { passwordChanged: true },
    })

    await sut.confirmPasswordReset(
      { kind: 'manager', email: 'rita@lava.test' },
      '123456',
      'senha-nova-456',
    )

    expect(request).toHaveBeenCalledWith({
      url: 'https://w.test/api/auth/managers/password-reset/confirm',
      method: 'post',
      body: {
        email: 'rita@lava.test',
        code: '123456',
        password: 'senha-nova-456',
      },
    })
  })

  it('saves the recovery e-mail with the new account token', async () => {
    const { request, sut } = make({
      statusCode: 200,
      body: { email: 'ana@mail.test' },
    })

    await expect(
      sut.saveRecoveryEmail('c1', 'ana@mail.test', 'fresh-token'),
    ).resolves.toBe('ana@mail.test')

    expect(request).toHaveBeenCalledWith({
      url: 'https://w.test/api/customers/c1/recovery-email',
      method: 'patch',
      body: { email: 'ana@mail.test' },
      headers: { Authorization: 'Bearer fresh-token' },
    })
  })

  it('surfaces other API errors as plain errors', async () => {
    const { sut } = make({
      statusCode: 401,
      body: { error: 'Código inválido ou expirado' },
    })

    const failure = sut.verifyCode({ phone: '92991234567' }, '000000')
    await expect(failure).rejects.toThrow('Código inválido ou expirado')
    await expect(failure).rejects.not.toBeInstanceOf(NeedsNameError)
  })
})
