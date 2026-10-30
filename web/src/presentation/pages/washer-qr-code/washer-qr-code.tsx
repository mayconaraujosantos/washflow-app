import { QRCodeSVG } from 'qrcode.react'
import { getReachableOrigin } from '@/main/config/reachable-origin'
import './washer-qr-code.css'

// Meant to be displayed on a screen or printed at the shop counter, not
// something a washer opens on their own phone from a link elsewhere.
// Scanning it is what takes them to /login/lavador, which is the only route
// that ever calls POST /api/auth/washers (see AuthenticateWasherController-
// Factory on the backend: the profile comes from which route/QR code was
// used, never from anything the client sends).
export function WasherQrCode() {
  const loginUrl = `${getReachableOrigin()}/login/lavador`

  return (
    <main className="qr-shell">
      <section className="qr-card">
        <span className="badge">Washflow · Lavador</span>
        <h1>Aponte a câmera do celular</h1>
        <p className="subtitle">
          Escaneie o QR Code abaixo para entrar como lavador.
        </p>

        <div className="qr-frame">
          <QRCodeSVG value={loginUrl} size={240} marginSize={2} />
        </div>

        <p className="qr-url">{loginUrl}</p>
      </section>
    </main>
  )
}
