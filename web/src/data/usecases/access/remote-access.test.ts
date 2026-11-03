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

  it('asks for the name when the code is right but the phone is new', async () => {
    const { sut } = make({
      statusCode: 400,
      body: { error: 'Informe seu nome para criar a conta', needsName: true },
    })

    await expect(
      sut.verifyCode('92991234567', '123456'),
    ).rejects.toBeInstanceOf(NeedsNameError)
  })

  it('surfaces other API errors as plain errors', async () => {
    const { sut } = make({
      statusCode: 401,
      body: { error: 'Código inválido ou expirado' },
    })

    const failure = sut.verifyCode('92991234567', '000000')
    await expect(failure).rejects.toThrow('Código inválido ou expirado')
    await expect(failure).rejects.not.toBeInstanceOf(NeedsNameError)
  })
})
