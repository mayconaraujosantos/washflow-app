import { beforeEach, describe, expect, it } from 'vitest'
import { UserModel } from '@/domain/models/user-model'
import {
  clearManagerSession,
  loadManagerSession,
  saveManagerSession,
} from './manager-session'

const session = {
  user: {
    id: '1',
    name: 'Ana',
    phone: '11999990000',
    profile: 'MANAGER',
    createdAt: '2026-01-01T00:00:00Z',
    active: true,
  } as UserModel,
  token: 'signed-token',
}

describe('manager-session', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('returns null when nothing was saved', () => {
    expect(loadManagerSession()).toBeNull()
  })

  it('round-trips a saved manager', () => {
    saveManagerSession(session)

    expect(loadManagerSession()).toEqual(session)
  })

  it('returns null for corrupted storage instead of throwing', () => {
    localStorage.setItem('washflow:manager-session', '{not json')

    expect(loadManagerSession()).toBeNull()
  })

  it('clears the saved manager', () => {
    saveManagerSession(session)
    clearManagerSession()

    expect(loadManagerSession()).toBeNull()
  })
})
