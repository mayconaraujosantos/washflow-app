import { beforeEach, describe, expect, it, vi } from 'vitest'
import { FetchHttpClient } from './fetch-http-client'
import { resetIdempotentPosts } from './idempotent-posts'

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

  describe('signed-in POSTs (double click and retry protection)', () => {
    const signIn = () =>
      localStorage.setItem(
        'washflow:manager-session',
        JSON.stringify({ token: 'manager-token' }),
      )
    const sale = { url: '/api/sales', method: 'post' as const, body: { total: 50 } }
    const keyOf = (call: number) =>
      (
        vi.mocked(fetch).mock.calls[call][1] as {
          headers: Record<string, string>
        }
      ).headers['Idempotency-Key']

    beforeEach(() => {
      resetIdempotentPosts()
      signIn()
    })

    it('sends an Idempotency-Key', async () => {
      await new FetchHttpClient().request(sale)

      expect(keyOf(0)).toMatch(/.{8,}/)
    })

    it('sends a double click only once and shares the response', async () => {
      const client = new FetchHttpClient()

      const [first, second] = await Promise.all([
        client.request(sale),
        client.request(sale),
      ])

      expect(fetch).toHaveBeenCalledTimes(1)
      expect(second).toEqual(first)
    })

    it('retries a failed request with the same key', async () => {
      vi.mocked(fetch)
        .mockRejectedValueOnce(new TypeError('Failed to fetch'))
        .mockResolvedValueOnce({
          status: 200,
          json: async () => ({ id: 'sale-1' }),
        } as Response)
      const client = new FetchHttpClient()

      await expect(client.request(sale)).rejects.toThrow('Failed to fetch')
      await client.request(sale)

      expect(keyOf(1)).toBe(keyOf(0))
    })

    it('uses a new key for the next operation after a success', async () => {
      const client = new FetchHttpClient()

      await client.request(sale)
      await client.request(sale)

      expect(fetch).toHaveBeenCalledTimes(2)
      expect(keyOf(1)).not.toBe(keyOf(0))
    })

    it('leaves reads and anonymous posts alone', async () => {
      await new FetchHttpClient().request({ url: '/api/sales', method: 'get' })
      localStorage.clear()
      await new FetchHttpClient().request({
        url: '/api/auth/customers',
        method: 'post',
        body: { phone: '92999990000' },
      })

      expect(keyOf(0)).toBeUndefined()
      expect(keyOf(1)).toBeUndefined()
    })
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
