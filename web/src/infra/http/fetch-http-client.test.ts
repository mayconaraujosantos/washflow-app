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
})
