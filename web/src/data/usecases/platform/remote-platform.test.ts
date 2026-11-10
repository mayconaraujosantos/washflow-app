import { describe, expect, it, vi } from 'vitest'
import type { HttpClient } from '@/data/protocols/http/http-client'
import { RemotePlatform } from './remote-platform'

const client = (statusCode: number, body: unknown) =>
  ({
    request: vi.fn().mockResolvedValue({ statusCode, body }),
  }) as HttpClient & { request: ReturnType<typeof vi.fn> }

describe('RemotePlatform', () => {
  it('searches the public directory', async () => {
    const http = client(200, [{ slug: 'lava-centro', name: 'Lava Centro' }])

    const found = await new RemotePlatform('', http).search('centro')

    expect(found[0].slug).toBe('lava-centro')
    expect(http.request).toHaveBeenCalledWith({
      url: '/api/establishments?query=centro',
      method: 'get',
      body: undefined,
    })
  })

  it('suspends a lava-jato', async () => {
    const http = client(200, { id: 'e1', active: false })

    await new RemotePlatform('', http).setActive('e1', false)

    expect(http.request).toHaveBeenCalledWith({
      url: '/api/admin/establishments/e1/active',
      method: 'patch',
      body: { active: false },
    })
  })

  it("resets the manager's password", async () => {
    const http = client(200, { id: 'e1', active: true })

    await new RemotePlatform('', http).resetManagerPassword(
      'e1',
      'nova-senha-123',
    )

    expect(http.request).toHaveBeenCalledWith({
      url: '/api/admin/establishments/e1/manager-password',
      method: 'patch',
      body: { password: 'nova-senha-123' },
    })
  })

  it('surfaces the API error', async () => {
    const http = client(409, {
      error: 'Já existe um lava-jato com o endereço x',
    })

    await expect(
      new RemotePlatform('', http).create({
        name: 'X',
        openingHour: 8,
        closingHour: 18,
        openOnSunday: false,
        maxVehiclesPerHour: 3,
        managerName: 'Ana',
        managerEmail: 'a@b.com',
        managerPassword: 'senha-forte',
      }),
    ).rejects.toThrow('Já existe um lava-jato com o endereço x')
  })
})
