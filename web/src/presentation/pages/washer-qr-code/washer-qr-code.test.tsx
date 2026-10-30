import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { WasherQrCode } from './washer-qr-code'

vi.mock('@/main/config/reachable-origin', () => ({
  getReachableOrigin: () => 'https://washflow.example.com',
}))

describe('WasherQrCode', () => {
  it('renders the LAVADOR badge', () => {
    render(<WasherQrCode />)

    expect(screen.getByText('Washflow · Lavador')).toBeInTheDocument()
  })

  it('points the QR code at /login/lavador on the reachable origin', () => {
    render(<WasherQrCode />)

    expect(
      screen.getByText('https://washflow.example.com/login/lavador'),
    ).toBeInTheDocument()
  })
})
