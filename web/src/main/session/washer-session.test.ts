import { beforeEach, describe, expect, it } from 'vitest'
import { UserModel } from '@/domain/models/user-model'
import {
  clearWasherSession,
  loadWasherSession,
  saveWasherSession,
} from './washer-session'

const session = {
  user: {
    id: '1',
    name: 'Ana',
    phone: '11999990000',
    profile: 'WASHER',
    createdAt: '2026-01-01T00:00:00Z',
    active: true,
  } as UserModel,
  token: 'signed-token',
}

describe('washer-session', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('returns null when nothing was saved', () => {
    expect(loadWasherSession()).toBeNull()
  })

  it('round-trips a saved washer', () => {
    saveWasherSession(session)

    expect(loadWasherSession()).toEqual(session)
  })

  it('returns null for corrupted storage instead of throwing', () => {
    localStorage.setItem('washflow:washer-session', '{not json')

    expect(loadWasherSession()).toBeNull()
  })

  it('clears the saved washer', () => {
    saveWasherSession(session)
    clearWasherSession()

    expect(loadWasherSession()).toBeNull()
  })
})
