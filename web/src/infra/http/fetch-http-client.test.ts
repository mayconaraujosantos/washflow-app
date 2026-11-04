import { beforeEach, describe, expect, it, vi } from 'vitest'
import { FetchHttpClient } from './fetch-http-client'

describe('FetchHttpClient', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        status: 200,
        json: async () => ({ status: 'ok' }),
      }),
    )
  })

  it.each([
    ['get', 'GET'],
    ['post', 'POST'],
    ['patch', 'PATCH'],
  ] as const)(
    'sends %s as the uppercase HTTP method %s',
    async (method, expectedMethod) => {
      await new FetchHttpClient().request({
        url: '/api/test',
        method,
      })

      expect(fetch).toHaveBeenCalledWith(
        '/api/test',
        expect.objectContaining({ method: expectedMethod }),
      )
    },
  )

  it('puts the request code in server error messages', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        status: 500,
        headers: new Headers(),
        json: async () => ({
          error: 'Internal server error',
          requestId: 'a1b2c3d4e5f6',
        }),
      }),
    )

    const response = await new FetchHttpClient().request({
      url: '/api/x',
      method: 'get',
    })

    expect(response.statusCode).toBe(500)
    expect((response.body as { error: string }).error).toContain('a1b2c3d4e5f6')
  })

  it('keeps client error messages untouched', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        status: 409,
        headers: new Headers(),
        json: async () => ({ error: 'Placa já cadastrada', requestId: 'zz' }),
      }),
    )

    const response = await new FetchHttpClient().request({
      url: '/api/x',
      method: 'post',
      body: {},
    })

    expect((response.body as { error: string }).error).toBe(
      'Placa já cadastrada',
    )
  })

  it('tells public screens which lava-jato the customer picked', async () => {
    localStorage.setItem(
      'washflow:establishment',
      JSON.stringify({ slug: 'lava-centro', name: 'Lava Centro' }),
    )

    await new FetchHttpClient().request({ url: '/api/services', method: 'get' })

    expect(fetch).toHaveBeenCalledWith(
      '/api/services',
      expect.objectContaining({
        headers: expect.objectContaining({ 'X-Establishment': 'lava-centro' }),
      }),
    )
  })
})
