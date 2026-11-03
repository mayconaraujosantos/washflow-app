import { beforeEach, describe, expect, it, vi } from 'vitest'
import { HttpClient, HttpStatusCode } from '@/data/protocols/http/http-client'
import { RemoteOperations } from './remote-operations'

describe('RemoteOperations', () => {
  const request = vi.fn()
  let operations: RemoteOperations

  beforeEach(() => {
    request.mockReset()
    request.mockResolvedValue({
      statusCode: HttpStatusCode.ok,
      body: {},
    })
    const httpClient: HttpClient = { request }
    operations = new RemoteOperations('https://washflow.test', httpClient)
  })

  it('registers a vehicle entry by plate', async () => {
    await operations.registerEntry({ plate: 'ABC1D23', servicePriceId: 2 })

    expect(request).toHaveBeenCalledWith({
      url: 'https://washflow.test/api/operations/entries',
      method: 'post',
      body: { plate: 'ABC1D23', servicePriceId: 2 },
    })
  })

  it('searches vehicles with an encoded query', async () => {
    await operations.searchVehicles('abc 1')

    expect(request).toHaveBeenCalledWith({
      url: 'https://washflow.test/api/operations/vehicles?query=abc+1',
      method: 'get',
      body: undefined,
    })
  })

  it('loads the agenda for a date range', async () => {
    await operations.loadAgenda('2026-11-01', '2026-11-30')

    expect(request).toHaveBeenCalledWith({
      url: 'https://washflow.test/api/operations/agenda?from=2026-11-01&to=2026-11-30',
      method: 'get',
      body: undefined,
    })
  })

  it('creates and lists sales', async () => {
    const sale = {
      type: 'QUICK_SALE' as const,
      items: [{ servicePriceId: 1, quantity: 2 }],
      payments: [{ method: 'PIX' as const, amount: 70 }],
    }
    await operations.createSale(sale)
    await operations.listSales('2026-11-03', '2026-11-03', 'BUDGET')

    expect(request).toHaveBeenNthCalledWith(1, {
      url: 'https://washflow.test/api/sales',
      method: 'post',
      body: sale,
    })
    expect(request).toHaveBeenNthCalledWith(2, {
      url: 'https://washflow.test/api/sales?from=2026-11-03&to=2026-11-03&type=BUDGET',
      method: 'get',
      body: undefined,
    })
  })

  it('surfaces the API error message', async () => {
    request.mockResolvedValueOnce({
      statusCode: 400,
      body: { error: 'Placa inválida: X' },
    })

    await expect(
      operations.registerEntry({ plate: 'X', servicePriceId: 1 }),
    ).rejects.toThrow('Placa inválida: X')
  })

  it('requests the manager report for the selected dates', async () => {
    await operations.loadReport('2026-10-01', '2026-10-26')

    expect(request).toHaveBeenCalledWith({
      url: 'https://washflow.test/api/operations/report?from=2026-10-01&to=2026-10-26',
      method: 'get',
      body: undefined,
    })
  })

  it('lists user accounts for manager administration', async () => {
    await operations.loadUsers()

    expect(request).toHaveBeenCalledWith({
      url: 'https://washflow.test/api/users',
      method: 'get',
      body: undefined,
    })
  })

  it('creates a washer account with its initial password', async () => {
    const account = {
      id: 'washer-1',
      name: 'Ana',
      phone: '11999990000',
      profile: 'WASHER',
      active: true,
      createdAt: '2026-10-27T12:00:00Z',
    }
    request.mockResolvedValueOnce({
      statusCode: 201,
      body: account,
    })

    await expect(
      operations.createWasher({
        name: 'Ana',
        phone: '11999990000',
        password: 'senha-segura',
      }),
    ).resolves.toEqual(account)
    expect(request).toHaveBeenCalledWith({
      url: 'https://washflow.test/api/users',
      method: 'post',
      body: {
        name: 'Ana',
        phone: '11999990000',
        password: 'senha-segura',
      },
    })
  })

  it('changes an account profile as the authenticated manager', async () => {
    await operations.updateUserProfile('user-1', 'manager-1', 'MANAGER')

    expect(request).toHaveBeenCalledWith({
      url: 'https://washflow.test/api/users/user-1/profile',
      method: 'patch',
      body: {
        managerId: 'manager-1',
        newProfile: 'MANAGER',
      },
    })
  })

  it('activates and deactivates user accounts', async () => {
    await operations.setUserActive('user-1', false)

    expect(request).toHaveBeenCalledWith({
      url: 'https://washflow.test/api/users/user-1/active',
      method: 'patch',
      body: { active: false },
    })
  })

  it('sends the pause reason and description', async () => {
    await operations.pause('order-1', 'OTHER', 'Repor material')

    expect(request).toHaveBeenCalledWith({
      url: 'https://washflow.test/api/service-orders/order-1/pause',
      method: 'patch',
      body: { reason: 'OTHER', description: 'Repor material' },
    })
  })

  it('calls the resume endpoint', async () => {
    await operations.resume('order-1')

    expect(request).toHaveBeenCalledWith({
      url: 'https://washflow.test/api/service-orders/order-1/resume',
      method: 'patch',
      body: undefined,
    })
  })
})
