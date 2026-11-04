import { describe, expect, it, vi } from 'vitest'
import { RemoteEstablishment } from './remote-establishment'

const make = (response: { statusCode: number; body?: unknown }) => {
  const request = vi.fn().mockResolvedValue(response)
  return {
    request,
    sut: new RemoteEstablishment('https://w.test', { request }),
  }
}

describe('RemoteEstablishment', () => {
  it('posts the setup wizard data and returns the owner session', async () => {
    const session = { token: 't', user: { id: '1' } }
    const { request, sut } = make({ statusCode: 200, body: session })
    const params = {
      establishmentName: 'Lava',
      openingHour: 8,
      closingHour: 18,
      openOnSunday: false,
      maxVehiclesPerHour: 3,
      ownerName: 'Ana',
      ownerEmail: 'ana@lava.com',
      ownerPassword: 'senha-forte',
    }

    await expect(sut.setup(params)).resolves.toEqual(session)
    expect(request).toHaveBeenCalledWith({
      url: 'https://w.test/api/setup',
      method: 'post',
      body: params,
    })
  })

  it('updates the settings with PUT', async () => {
    const { request, sut } = make({ statusCode: 200, body: {} })
    const params = {
      name: 'Lava',
      openingHour: 7,
      closingHour: 19,
      openOnSunday: true,
      maxVehiclesPerHour: 5,
    }

    await sut.update(params)

    expect(request).toHaveBeenCalledWith({
      url: 'https://w.test/api/establishment',
      method: 'put',
      body: params,
    })
  })

  it('surfaces the API error', async () => {
    const { sut } = make({
      statusCode: 403,
      body: { error: 'Código de configuração inválido' },
    })

    await expect(sut.setupStatus()).rejects.toThrow(
      'Código de configuração inválido',
    )
  })
})
