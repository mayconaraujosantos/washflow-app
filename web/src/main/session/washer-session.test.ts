import { beforeEach, describe, expect, it } from 'vitest'
import { UserModel } from '@/domain/models/user-model'
import {
  clearWasherSession,
  loadWasherSession,
  saveWasherSession,
} from './washer-session'

const user: UserModel = {
  id: '1',
  name: 'Ana',
  phone: '11999990000',
  profile: 'WASHER',
  createdAt: '2026-01-01T00:00:00Z',
}

describe('washer-session', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('returns null when nothing was saved', () => {
    expect(loadWasherSession()).toBeNull()
  })

  it('round-trips a saved washer', () => {
    saveWasherSession(user)

    expect(loadWasherSession()).toEqual(user)
  })

  it('returns null for corrupted storage instead of throwing', () => {
    localStorage.setItem('washflow:washer-session', '{not json')

    expect(loadWasherSession()).toBeNull()
  })

  it('clears the saved washer', () => {
    saveWasherSession(user)
    clearWasherSession()

    expect(loadWasherSession()).toBeNull()
  })
})
