import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { ManagerContext, type ManagerData } from './manager-context'
import { WhatsappSection } from './whatsapp-section'

const renderSection = (establishment: Record<string, unknown>) =>
  render(
    <ManagerContext.Provider
      value={{ establishment, notify: vi.fn() } as unknown as ManagerData}
    >
      <WhatsappSection />
    </ManagerContext.Provider>,
  )

describe('WhatsappSection', () => {
  it('shows the QR code to link a disconnected number', async () => {
    const connectWhatsapp = vi.fn().mockResolvedValue({
      state: 'WAITING_QR_CODE',
      qrCode: 'data:image/png;base64,QR',
    })
    renderSection({
      whatsappStatus: vi
        .fn()
        .mockResolvedValue({ state: 'DISCONNECTED', qrCode: null }),
      connectWhatsapp,
    })

    fireEvent.click(await screen.findByText('Conectar WhatsApp'))

    const qr = await screen.findByAltText('QR Code para conectar o WhatsApp')
    expect(qr).toHaveAttribute('src', 'data:image/png;base64,QR')
    expect(connectWhatsapp).toHaveBeenCalledTimes(1)
  })

  it('offers to disconnect a linked number', async () => {
    const disconnectWhatsapp = vi
      .fn()
      .mockResolvedValue({ state: 'DISCONNECTED', qrCode: null })
    renderSection({
      whatsappStatus: vi
        .fn()
        .mockResolvedValue({ state: 'CONNECTED', qrCode: null }),
      disconnectWhatsapp,
    })

    fireEvent.click(await screen.findByText('Desconectar'))

    expect(await screen.findByText('Conectar WhatsApp')).toBeInTheDocument()
    expect(disconnectWhatsapp).toHaveBeenCalledTimes(1)
  })

  it('explains when the server has no WhatsApp', async () => {
    renderSection({
      whatsappStatus: vi
        .fn()
        .mockRejectedValue(
          new Error('WhatsApp não está configurado neste servidor'),
        ),
    })

    expect(
      await screen.findByText('WhatsApp não está configurado neste servidor'),
    ).toBeInTheDocument()
    expect(screen.queryByText('Conectar WhatsApp')).not.toBeInTheDocument()
  })
})
