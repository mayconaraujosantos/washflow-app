import { SubmitEvent, useEffect, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import type { AuthenticatedSessionModel } from '@/domain/models/authenticated-session-model'
import { NeedsNameError, type Access } from '@/domain/usecases/access'
import { saveCustomerSession } from '@/main/session/customer-session'
import { AuthLayout } from '@/presentation/components/ui/app-chrome'
import { PhoneField, TextField } from '@/presentation/components/ui/auth-fields'
import { Notice } from '@/presentation/components/ui/blocks'
import { errorMessage } from '@/presentation/helpers/format'

type OnSession = (session: AuthenticatedSessionModel, isNew: boolean) => void

/** Phone -> 6-digit code by WhatsApp -> session (asks the name for a new phone). */
export function WhatsAppLogin({
  access,
  onSession,
  onCancel,
}: Readonly<{ access: Access; onSession: OnSession; onCancel: () => void }>) {
  const [phone, setPhone] = useState('')
  const [code, setCode] = useState('')
  const [name, setName] = useState('')
  const [sent, setSent] = useState(false)
  const [needsName, setNeedsName] = useState(false)
  const [info, setInfo] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const send = async (event?: SubmitEvent) => {
    event?.preventDefault()
    setError(null)
    setLoading(true)
    try {
      const seconds = await access.requestCode(phone)
      setSent(true)
      setInfo(
        `Enviamos um código para o seu WhatsApp. Ele vale por ${Math.round(seconds / 60)} minutos.`,
      )
    } catch (cause) {
      setError(errorMessage(cause, 'Não foi possível enviar o código'))
    } finally {
      setLoading(false)
    }
  }

  const verify = async (event: SubmitEvent) => {
    event.preventDefault()
    setError(null)
    setLoading(true)
    try {
      const session = await access.verifyCode(
        phone,
        code,
        needsName ? name.trim() : undefined,
      )
      saveCustomerSession(session)
      onSession(session, needsName)
    } catch (cause) {
      if (cause instanceof NeedsNameError) {
        setNeedsName(true)
        setInfo('Primeiro acesso: como podemos te chamar?')
      } else {
        setError(errorMessage(cause, 'Código inválido ou expirado'))
      }
    } finally {
      setLoading(false)
    }
  }

  if (!sent) {
    return (
      <form className="form" onSubmit={(event) => void send(event)}>
        <PhoneField label="Seu WhatsApp" value={phone} onChange={setPhone} />
        <Notice tone="error">{error}</Notice>
        <button type="submit" className="btn btn-dark" disabled={loading}>
          {loading ? 'Enviando...' : 'Enviar código'}
        </button>
        <div className="auth-links">
          <button type="button" onClick={onCancel}>
            Voltar
          </button>
        </div>
      </form>
    )
  }

  return (
    <form className="form" onSubmit={(event) => void verify(event)}>
      {info && <p className="subtitle">{info}</p>}
      <div className="field">
        <label htmlFor="otp-code" className="visually-hidden">
          Código
        </label>
        <input
          id="otp-code"
          className="input pin-input"
          inputMode="numeric"
          autoComplete="one-time-code"
          maxLength={6}
          placeholder="Código"
          value={code}
          onChange={(event) =>
            setCode(event.target.value.replace(/\D/g, '').slice(0, 6))
          }
          required
        />
      </div>
      {needsName && (
        <TextField
          label="Seu nome"
          value={name}
          onChange={setName}
          autoComplete="name"
        />
      )}
      <Notice tone="error">{error}</Notice>
      <button
        type="submit"
        className="btn btn-dark"
        disabled={loading || code.length !== 6}
      >
        {loading ? 'Verificando...' : 'Entrar'}
      </button>
      <div className="auth-links">
        <button type="button" onClick={() => void send()} disabled={loading}>
          Reenviar código
        </button>
        <button type="button" onClick={onCancel}>
          Voltar
        </button>
      </div>
    </form>
  )
}

type GoogleId = {
  accounts: {
    id: {
      initialize: (options: {
        client_id: string
        callback: (response: { credential: string }) => void
      }) => void
      renderButton: (
        element: HTMLElement,
        options: Record<string, string>,
      ) => void
    }
  }
}

const GIS_SRC = 'https://accounts.google.com/gsi/client'

function loadGoogleScript(): Promise<GoogleId> {
  const existing = (window as unknown as { google?: GoogleId }).google
  if (existing?.accounts) return Promise.resolve(existing)
  return new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = GIS_SRC
    script.async = true
    script.onload = () =>
      resolve((window as unknown as { google: GoogleId }).google)
    script.onerror = () =>
      reject(new Error('Não foi possível carregar o Google'))
    document.head.appendChild(script)
  })
}

/** Google Identity Services button; the API validates the ID token. */
export function GoogleButton({
  access,
  clientId,
  onSession,
  onError,
}: Readonly<{
  access: Access
  clientId: string
  onSession: OnSession
  onError: (message: string) => void
}>) {
  const container = useRef<HTMLDivElement>(null)

  useEffect(() => {
    let active = true
    loadGoogleScript()
      .then((google) => {
        if (!active || !container.current) return
        google.accounts.id.initialize({
          client_id: clientId,
          callback: ({ credential }) => {
            access
              .googleLogin(credential)
              .then((session) => {
                saveCustomerSession(session)
                onSession(session, false)
              })
              .catch((cause) =>
                onError(
                  errorMessage(cause, 'Não foi possível entrar com Google'),
                ),
              )
          },
        })
        google.accounts.id.renderButton(container.current, {
          theme: 'filled_black',
          size: 'large',
          shape: 'pill',
          text: 'continue_with',
          locale: 'pt-BR',
        })
      })
      .catch((cause) => onError(errorMessage(cause, 'Google indisponível')))
    return () => {
      active = false
    }
  }, [access, clientId, onSession, onError])

  return <div ref={container} className="google-button" />
}

/** Target of the WhatsApp magic link: /entrar/whatsapp?telefone=...&codigo=... */
export function MagicLinkPage({ access }: Readonly<{ access: Access }>) {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const phone = params.get('telefone') ?? ''
  const code = params.get('codigo') ?? ''
  const [needsName, setNeedsName] = useState(false)
  const [name, setName] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const attempted = useRef(false)

  const verify = async (withName?: string) => {
    setError(null)
    setLoading(true)
    try {
      saveCustomerSession(await access.verifyCode(phone, code, withName))
      navigate('/cliente', { replace: true })
    } catch (cause) {
      if (cause instanceof NeedsNameError) setNeedsName(true)
      else setError(errorMessage(cause, 'Link inválido ou expirado'))
      setLoading(false)
    }
  }

  useEffect(() => {
    if (attempted.current) return
    attempted.current = true
    const timer = window.setTimeout(() => void verify(), 0)
    return () => window.clearTimeout(timer)
    // eslint-disable-next-line react-hooks/exhaustive-deps -- runs once for the link
  }, [])

  return (
    <AuthLayout
      title="Entrando pelo WhatsApp"
      footer={
        <a href="/login/cliente" className="auth-back">
          Entrar de outro jeito
        </a>
      }
    >
      {needsName ? (
        <form
          className="form"
          onSubmit={(event) => {
            event.preventDefault()
            void verify(name.trim())
          }}
        >
          <p className="subtitle">Primeiro acesso: como podemos te chamar?</p>
          <TextField
            label="Seu nome"
            value={name}
            onChange={setName}
            autoComplete="name"
          />
          <button type="submit" className="btn btn-dark" disabled={loading}>
            Criar conta e entrar
          </button>
        </form>
      ) : (
        <Notice tone={error ? 'error' : 'info'}>
          {error ?? (loading ? 'Confirmando seu código...' : null)}
        </Notice>
      )}
    </AuthLayout>
  )
}
