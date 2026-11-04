import { SubmitEvent, useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import type { AuthenticatedSessionModel } from '@/domain/models/authenticated-session-model'
import type { Access } from '@/domain/usecases/access'
import { AuthenticateWasher } from '@/domain/usecases/authenticate-washer'
import { saveWasherSession } from '@/main/session/washer-session'
import { StaffLogin } from '@/presentation/components/staff-login'
import { UiIcon } from '@/presentation/components/ui-icon'
import { AuthLayout } from '@/presentation/components/ui/app-chrome'
import { PhoneField } from '@/presentation/components/ui/auth-fields'
import { Notice } from '@/presentation/components/ui/blocks'
import { errorMessage } from '@/presentation/helpers/format'
import { badgeTokenFrom } from './badge-token'

type Props = Readonly<{
  authenticateWasher: AuthenticateWasher
  // Badge (PIN/QR) login; without it the page offers only phone + password.
  access?: Access
}>

export function WasherLogin({ authenticateWasher, access }: Props) {
  const [usePassword, setUsePassword] = useState(!access)

  if (usePassword || !access) {
    return (
      <StaffLogin
        title="Entrar como lavador"
        subtitle="Entre com o telefone e a senha fornecidos pelo gerente."
        passwordLabel="Senha (mínimo 8 caracteres)"
        panelPath="/lavador"
        panelLabel="Abrir painel do lavador"
        authenticate={(credentials) =>
          authenticateWasher.auth({
            phone: 'phone' in credentials ? credentials.phone : '',
            password: credentials.password,
          })
        }
        saveSession={saveWasherSession}
        alternative={
          access && (
            <button type="button" onClick={() => setUsePassword(false)}>
              Entrar com PIN ou crachá
            </button>
          )
        }
      />
    )
  }
  return (
    <BadgeLogin access={access} onUsePassword={() => setUsePassword(true)} />
  )
}

/** Phone + 6-digit PIN, or the badge QR (scanned here or opened from the phone camera). */
function BadgeLogin({
  access,
  onUsePassword,
}: Readonly<{ access: Access; onUsePassword: () => void }>) {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const badgeFromLink = searchParams.get('cracha')
  const [phone, setPhone] = useState('')
  const [pin, setPin] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(Boolean(badgeFromLink))
  const [scanning, setScanning] = useState(false)

  const finish = useCallback(
    (session: AuthenticatedSessionModel) => {
      saveWasherSession(session)
      navigate('/lavador', { replace: true })
    },
    [navigate],
  )

  const loginWithBadge = useCallback(
    async (token: string) => {
      setError(null)
      setLoading(true)
      try {
        finish(await access.washerBadgeLogin(token))
      } catch (cause) {
        setError(errorMessage(cause, 'Crachá inválido'))
        setLoading(false)
      }
    },
    [access, finish],
  )

  // The badge QR is a link to this page with ?cracha=<token>.
  useEffect(() => {
    if (!badgeFromLink) return
    const timer = window.setTimeout(() => void loginWithBadge(badgeFromLink), 0)
    return () => window.clearTimeout(timer)
  }, [badgeFromLink, loginWithBadge])

  const submitPin = async (event: SubmitEvent) => {
    event.preventDefault()
    setError(null)
    setLoading(true)
    try {
      finish(await access.washerPinLogin(phone, pin))
    } catch (cause) {
      setError(errorMessage(cause, 'Telefone ou PIN inválidos'))
      setLoading(false)
    }
  }

  return (
    <AuthLayout
      title="Entrar como lavador"
      subtitle="Use o PIN do seu crachá ou leia o QR Code dele."
      footer={
        <a href="/" className="auth-back">
          Voltar para o início
        </a>
      }
    >
      {scanning ? (
        <BadgeScanner
          onToken={(token) => {
            setScanning(false)
            void loginWithBadge(token)
          }}
          onCancel={() => setScanning(false)}
        />
      ) : (
        <form className="form" onSubmit={submitPin}>
          <PhoneField value={phone} onChange={setPhone} />
          <div className="field">
            <label htmlFor="washer-pin" className="visually-hidden">
              PIN
            </label>
            <input
              id="washer-pin"
              className="input pin-input"
              inputMode="numeric"
              autoComplete="one-time-code"
              pattern="\d{6}"
              maxLength={6}
              placeholder="PIN (6 dígitos)"
              value={pin}
              onChange={(event) =>
                setPin(event.target.value.replace(/\D/g, '').slice(0, 6))
              }
              required
            />
          </div>
          <Notice tone="error">{error}</Notice>
          <button
            type="submit"
            className="btn btn-dark"
            disabled={loading || pin.length !== 6}
          >
            {loading ? 'Entrando...' : 'Entrar com PIN'}
          </button>
          <button
            type="button"
            className="btn btn-outline"
            onClick={() => setScanning(true)}
          >
            <UiIcon name="qr" size={18} />
            Ler QR do crachá
          </button>
          <div className="auth-links">
            <button type="button" onClick={onUsePassword}>
              Entrar com senha
            </button>
          </div>
        </form>
      )}
    </AuthLayout>
  )
}

type Detector = {
  detect: (source: CanvasImageSource) => Promise<{ rawValue: string }[]>
}
type DetectorConstructor = new (options: { formats: string[] }) => Detector

/**
 * In-app QR reader using the browser's BarcodeDetector (Chrome on Android).
 * Elsewhere the phone's own camera app opens the badge link, which logs in too.
 */
function BadgeScanner({
  onToken,
  onCancel,
}: Readonly<{ onToken: (token: string) => void; onCancel: () => void }>) {
  const videoRef = useRef<HTMLVideoElement>(null)
  const Detector = (
    window as unknown as { BarcodeDetector?: DetectorConstructor }
  ).BarcodeDetector
  const [error, setError] = useState<string | null>(
    Detector
      ? null
      : 'Este navegador não lê QR Codes. Abra a câmera do celular e aponte para o crachá: o link já entra no painel.',
  )

  useEffect(() => {
    if (!Detector) return
    let stream: MediaStream | null = null
    let stopped = false
    const detector = new Detector({ formats: ['qr_code'] })
    const scan = async () => {
      const video = videoRef.current
      if (stopped || !video) return
      try {
        const codes = await detector.detect(video)
        const token = codes
          .map((code) => badgeTokenFrom(code.rawValue))
          .find(Boolean)
        if (token) {
          onToken(token)
          return
        }
      } catch {
        // Frame not ready yet; try again.
      }
      window.setTimeout(() => void scan(), 300)
    }
    navigator.mediaDevices
      ?.getUserMedia({ video: { facingMode: 'environment' } })
      .then((media) => {
        stream = media
        if (videoRef.current) {
          videoRef.current.srcObject = media
          void videoRef.current.play()
          void scan()
        }
      })
      .catch(() => setError('Não foi possível abrir a câmera.'))
    return () => {
      stopped = true
      stream?.getTracks().forEach((track) => track.stop())
    }
  }, [Detector, onToken])

  return (
    <div className="form">
      {Detector && !error && (
        <video ref={videoRef} className="scanner-video" muted playsInline />
      )}
      <Notice tone="info">{error}</Notice>
      <button type="button" className="btn btn-outline" onClick={onCancel}>
        Voltar para o PIN
      </button>
    </div>
  )
}
