import { describe, expect, it, vi } from 'vitest'
import { HttpClient, HttpStatusCode } from '@/data/protocols/http/http-client'
import { AuthenticateWasher } from '@/domain/usecases/authenticate-washer'
import { RemoteAuthenticateWasher } from './remote-authenticate-washer'

const makeSut = () => {
  const httpClient: HttpClient<AuthenticateWasher.Model> = {
    request: vi.fn(),
  }
  const sut = new RemoteAuthenticateWasher(
    'http://api.washflow.local/api/auth/washers',
    httpClient,
  )

  return { sut, httpClient }
}

describe('RemoteAuthenticateWasher', () => {
  const params = { phone: '11999990000', name: 'Ana', password: 'senha-forte' }
  const session = {
    user: {
      id: '1',
      name: 'Ana',
      phone: '11999990000',
      profile: 'WASHER',
      createdAt: '2026-01-01T00:00:00Z',
      active: true,
    },
    token: 'signed-token',
  }

  it('posts credentials to the given url', async () => {
    const { sut, httpClient } = makeSut()
    vi.mocked(httpClient.request).mockResolvedValueOnce({
      statusCode: HttpStatusCode.ok,
      body: session as never,
    })

    await sut.auth(params)

    expect(httpClient.request).toHaveBeenCalledWith({
      url: 'http://api.washflow.local/api/auth/washers',
      method: 'post',
      body: params,
    })
  })

  it('returns the authenticated washer on 200', async () => {
    const { sut, httpClient } = makeSut()
    vi.mocked(httpClient.request).mockResolvedValueOnce({
      statusCode: HttpStatusCode.ok,
      body: session as never,
    })

    const result = await sut.auth(params)

    expect(result).toEqual(session)
  })

  it('throws the server message on 400', async () => {
    const { sut, httpClient } = makeSut()
    vi.mocked(httpClient.request).mockResolvedValueOnce({
      statusCode: HttpStatusCode.badRequest,
      body: { error: 'phone is required' } as never,
    })

    await expect(sut.auth({ ...params, phone: '' })).rejects.toThrow(
      'phone is required',
    )
  })

  it('throws a profile-conflict message on 409', async () => {
    const { sut, httpClient } = makeSut()
    vi.mocked(httpClient.request).mockResolvedValueOnce({
      statusCode: HttpStatusCode.conflict,
      body: {} as never,
    })

    await expect(sut.auth(params)).rejects.toThrow(
      'Este telefone já está cadastrado com outro perfil',
    )
  })

  it('throws a generic message on any other status', async () => {
    const { sut, httpClient } = makeSut()
    vi.mocked(httpClient.request).mockResolvedValueOnce({
      statusCode: HttpStatusCode.serverError,
      body: {} as never,
    })

    await expect(sut.auth(params)).rejects.toThrow('Erro inesperado ao entrar')
  })
})
