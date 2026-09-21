import { describe, expect, it, vi } from 'vitest'
import { HttpClient, HttpStatusCode } from '@/data/protocols/http/http-client'
import { AuthenticateManager } from '@/domain/usecases/authenticate-manager'
import { RemoteAuthenticateManager } from './remote-authenticate-manager'

const makeSut = () => {
  const httpClient: HttpClient<AuthenticateManager.Model> = {
    request: vi.fn(),
  }
  const sut = new RemoteAuthenticateManager(
    'http://api.washflow.local/api/auth/managers',
    httpClient,
  )

  return { sut, httpClient }
}

describe('RemoteAuthenticateManager', () => {
  it('posts phone and name to the given url', async () => {
    const { sut, httpClient } = makeSut()
    vi.mocked(httpClient.request).mockResolvedValueOnce({
      statusCode: HttpStatusCode.ok,
      body: { id: '1', name: 'Ana', phone: '11999990000' } as never,
    })

    await sut.auth({ phone: '11999990000', name: 'Ana' })

    expect(httpClient.request).toHaveBeenCalledWith({
      url: 'http://api.washflow.local/api/auth/managers',
      method: 'post',
      body: { phone: '11999990000', name: 'Ana' },
    })
  })

  it('returns the authenticated manager on 200', async () => {
    const { sut, httpClient } = makeSut()
    const user = {
      id: '1',
      name: 'Ana',
      phone: '11999990000',
      profile: 'MANAGER',
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

  it('throws a not-yet-a-manager message on 403', async () => {
    const { sut, httpClient } = makeSut()
    vi.mocked(httpClient.request).mockResolvedValueOnce({
      statusCode: HttpStatusCode.forbidden,
      body: {} as never,
    })

    await expect(
      sut.auth({ phone: '11999990000', name: 'Ana' }),
    ).rejects.toThrow('Este telefone ainda não foi promovido a gerente')
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
