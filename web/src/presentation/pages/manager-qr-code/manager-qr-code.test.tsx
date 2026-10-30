import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { ManagerQrCode } from './manager-qr-code'

vi.mock('@/main/config/reachable-origin', () => ({
  getReachableOrigin: () => 'https://washflow.example.com',
}))

describe('ManagerQrCode', () => {
  it('renders the GERENTE badge', () => {
    render(<ManagerQrCode />)

    expect(screen.getByText('Washflow · Gerente')).toBeInTheDocument()
  })

  it('points the QR code at /login/gerente on the reachable origin', () => {
    render(<ManagerQrCode />)

    expect(
      screen.getByText('https://washflow.example.com/login/gerente'),
    ).toBeInTheDocument()
  })
})
