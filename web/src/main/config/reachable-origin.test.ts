import { describe, expect, it } from 'vitest'
import { getReachableOrigin } from '@/main/config/reachable-origin'

describe('getReachableOrigin', () => {
  it('returns window.location.origin as-is when not on localhost', () => {
    Object.defineProperty(window, 'location', {
      value: {
        hostname: 'washflow.example.com',
        origin: 'https://washflow.example.com',
        protocol: 'https:',
        port: '',
      },
      writable: true,
    })

    expect(getReachableOrigin()).toBe('https://washflow.example.com')
  })
})
