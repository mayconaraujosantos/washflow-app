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
  it('posts phone and name to the given url', async () => {
    const { sut, httpClient } = makeSut()
    vi.mocked(httpClient.request).mockResolvedValueOnce({
      statusCode: HttpStatusCode.ok,
      body: { id: '1', name: 'Ana', phone: '11999990000' } as never,
    })

    await sut.auth({ phone: '11999990000', name: 'Ana' })

    expect(httpClient.request).toHaveBeenCalledWith({
      url: 'http://api.washflow.local/api/auth/washers',
      method: 'post',
      body: { phone: '11999990000', name: 'Ana' },
    })
  })

  it('returns the authenticated washer on 200', async () => {
    const { sut, httpClient } = makeSut()
    const user = {
      id: '1',
      name: 'Ana',
      phone: '11999990000',
      profile: 'WASHER',
      createdAt: '2026-01-01T00:00:00Z',
    }
    vi.mocked(httpClient.request).mockResolvedValueOnce({
      statusCode: HttpStatusCode.ok,
      body: user as never,
    })

    const result = await sut.auth({ phone: '11999990000', name: 'Ana' })

    expect(result).toEqual(user)
  })

  it('throws the server message on 400', async () => {
    const { sut, httpClient } = makeSut()
    vi.mocked(httpClient.request).mockResolvedValueOnce({
      statusCode: HttpStatusCode.badRequest,
      body: { error: 'phone is required' } as never,
    })

    await expect(sut.auth({ phone: '', name: 'Ana' })).rejects.toThrow(
      'phone is required',
    )
  })

  it('throws a profile-conflict message on 409', async () => {
    const { sut, httpClient } = makeSut()
    vi.mocked(httpClient.request).mockResolvedValueOnce({
      statusCode: HttpStatusCode.conflict,
      body: {} as never,
    })

    await expect(
      sut.auth({ phone: '11999990000', name: 'Ana' }),
    ).rejects.toThrow('Este telefone já está cadastrado com outro perfil')
  })

  it('throws a generic message on any other status', async () => {
    const { sut, httpClient } = makeSut()
    vi.mocked(httpClient.request).mockResolvedValueOnce({
      statusCode: HttpStatusCode.serverError,
      body: {} as never,
    })

    await expect(
      sut.auth({ phone: '11999990000', name: 'Ana' }),
    ).rejects.toThrow('Erro inesperado ao entrar')
  })
})
