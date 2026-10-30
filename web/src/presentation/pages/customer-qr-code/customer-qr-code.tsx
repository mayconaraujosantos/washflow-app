import { QRCodeSVG } from 'qrcode.react'
import { getReachableOrigin } from '@/main/config/reachable-origin'
import './customer-qr-code.css'

// Meant to be displayed on a screen or printed at the shop counter - not
// something a customer opens on their own phone. Scanning it is what takes
// them to /login/cliente, which is the only route that ever calls
// POST /api/auth/customers (see AuthenticateCustomerController on the
// backend: the profile comes from which route/QR code was used, never from
// anything the client sends).
export function CustomerQrCode() {
  const loginUrl = `${getReachableOrigin()}/login/cliente`

  return (
    <main className="qr-shell">
      <section className="qr-card">
        <span className="badge">Washflow · Cliente</span>
        <h1>Aponte a câmera do celular</h1>
        <p className="subtitle">
          Escaneie o QR Code abaixo para agendar sua lavagem.
        </p>

        <div className="qr-frame">
          <QRCodeSVG value={loginUrl} size={240} marginSize={2} />
        </div>

        <p className="qr-url">{loginUrl}</p>
      </section>
    </main>
  )
}
