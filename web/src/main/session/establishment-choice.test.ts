import { beforeEach, describe, expect, it } from 'vitest'
import {
  adoptEstablishmentFromUrl,
  clearEstablishmentChoice,
  loadEstablishmentChoice,
  saveEstablishmentChoice,
} from './establishment-choice'

describe('establishment choice', () => {
  beforeEach(() => {
    localStorage.clear()
    window.history.replaceState(null, '', '/')
  })

  it('remembers the picked lava-jato', () => {
    saveEstablishmentChoice({ slug: 'lava-centro', name: 'Lava Centro' })

    expect(loadEstablishmentChoice()).toEqual({
      slug: 'lava-centro',
      name: 'Lava Centro',
    })
    clearEstablishmentChoice()
    expect(loadEstablishmentChoice()).toBeNull()
  })

  it('ignores tampered values', () => {
    localStorage.setItem(
      'washflow:establishment',
      JSON.stringify({ slug: 'x"; drop', name: 'X' }),
    )
    expect(loadEstablishmentChoice()).toBeNull()
  })

  it('adopts ?lavajato= from the shop link and cleans the address bar', () => {
    window.history.replaceState(null, '', '/?lavajato=Lava-Centro&x=1')

    adoptEstablishmentFromUrl()

    expect(loadEstablishmentChoice()).toEqual({ slug: 'lava-centro', name: '' })
    expect(window.location.search).toBe('?x=1')
  })
})
