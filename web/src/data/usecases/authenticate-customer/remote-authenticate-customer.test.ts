import { describe, expect, it, vi } from 'vitest'
import { HttpClient, HttpStatusCode } from '@/data/protocols/http/http-client'
import { AuthenticateCustomer } from '@/domain/usecases/authenticate-customer'
import { RemoteAuthenticateCustomer } from './remote-authenticate-customer'

const makeSut = () => {
  const httpClient: HttpClient<AuthenticateCustomer.Model> = {
    request: vi.fn(),
  }
  const sut = new RemoteAuthenticateCustomer(
    'http://api.washflow.local/api/auth/customers',
    httpClient,
  )

  return { sut, httpClient }
}

describe('RemoteAuthenticateCustomer', () => {
  const params = { phone: '92999990000', password: 'senha-forte' }

  it('throws the server message on 401', async () => {
    const { sut, httpClient } = makeSut()
    vi.mocked(httpClient.request).mockResolvedValueOnce({
      statusCode: HttpStatusCode.unauthorized,
      body: { error: 'Telefone ou senha inválidos' } as never,
    })

    await expect(sut.auth(params)).rejects.toThrow(
      'Telefone ou senha inválidos',
    )
  })

  it('tells the user to wait when sign-in is locked (429)', async () => {
    const { sut, httpClient } = makeSut()
    vi.mocked(httpClient.request).mockResolvedValueOnce({
      statusCode: HttpStatusCode.tooManyRequests,
      body: {
        error: 'Muitas tentativas de acesso. Tente de novo em 15 minutos.',
      } as never,
    })

    await expect(sut.auth(params)).rejects.toThrow(
      'Muitas tentativas de acesso. Tente de novo em 15 minutos.',
    )
  })
})
