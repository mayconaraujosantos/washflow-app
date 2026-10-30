import { beforeEach, describe, expect, it } from 'vitest'
import { loadAccessToken } from './access-token'

const sessions = {
  customer: 'washflow:customer-session',
  washer: 'washflow:washer-session',
  manager: 'washflow:manager-session',
}

function saveSession(key: string, token: string) {
  localStorage.setItem(key, JSON.stringify({ token }))
}

function setPath(path: string) {
  window.history.replaceState({}, '', path)
}

describe('loadAccessToken', () => {
  beforeEach(() => {
    localStorage.clear()
    setPath('/')
  })

  it('uses the washer token on washer routes even if a customer session exists', () => {
    saveSession(sessions.customer, 'customer-token')
    saveSession(sessions.washer, 'washer-token')
    setPath('/lavador')

    expect(loadAccessToken()).toBe('washer-token')
  })

  it('uses the manager token on manager login and dashboard routes', () => {
    saveSession(sessions.manager, 'manager-token')
    saveSession(sessions.washer, 'washer-token')
    setPath('/login/gerente')

    expect(loadAccessToken()).toBe('manager-token')
  })

  it('does not choose another profile token when the active role is not signed in', () => {
    saveSession(sessions.customer, 'customer-token')
    setPath('/lavador')

    expect(loadAccessToken()).toBeNull()
  })

  it('uses the only available session on an unscoped route', () => {
    saveSession(sessions.customer, 'customer-token')

    expect(loadAccessToken()).toBe('customer-token')
  })

  it('does not guess a profile when multiple sessions exist on an unscoped route', () => {
    saveSession(sessions.customer, 'customer-token')
    saveSession(sessions.washer, 'washer-token')

    expect(loadAccessToken()).toBeNull()
  })
})
