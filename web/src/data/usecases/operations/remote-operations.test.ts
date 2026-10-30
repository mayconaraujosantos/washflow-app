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
