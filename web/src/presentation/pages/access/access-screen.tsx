import { SubmitEvent, useCallback, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import type { AuthOptionsModel } from '@/domain/models/access-model'
import type { AuthenticatedSessionModel } from '@/domain/models/authenticated-session-model'
import type { Access } from '@/domain/usecases/access'
import type { AuthenticateCustomer } from '@/domain/usecases/authenticate-customer'
import { saveCustomerSession } from '@/main/session/customer-session'
import { UiIcon } from '@/presentation/components/ui-icon'
import { BrandLogo } from '@/presentation/components/ui/app-chrome'
import {
  PasswordField,
  PhoneField,
  TextField,
} from '@/presentation/components/ui/auth-fields'
import { Notice } from '@/presentation/components/ui/blocks'
import { usePwaInstall } from '@/presentation/components/ui/use-pwa-install'
import { errorMessage } from '@/presentation/helpers/format'
import { GoogleButton, WhatsAppLogin } from './passwordless'

type Mode = 'signup' | 'signin' | 'forgot' | 'whatsapp'

type Props = Readonly<{
  authenticateCustomer: AuthenticateCustomer
  // WhatsApp code / Google sign-in, shown when the API enables them.
  access?: Access
  initialMode?: 'signup' | 'signin'
}>

/**
 * Customer access card (target of the "Cliente > Entrar" role card and of the
 * customer QR code): create an account with name, phone and password, or sign
 * in with phone and password. Staff accounts are provisioned by a manager and
 * use their own login pages, so there is no profile choice here.
 */
export function AccessScreen({
  authenticateCustomer,
  access,
  initialMode = 'signup',
}: Props) {
  const navigate = useNavigate()
  const [mode, setMode] = useState<Mode>(initialMode)
  const [name, setName] = useState('')
  const [phone, setPhone] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [welcome, setWelcome] = useState(false)
  const [options, setOptions] = useState<AuthOptionsModel | null>(null)

  useEffect(() => {
    if (!access) return
    let active = true
    access
      .options()
      .then((loaded) => {
        if (active) setOptions(loaded)
      })
      .catch(() => {
        // Without options only phone + password is offered.
      })
    return () => {
      active = false
    }
  }, [access])

  const onPasswordless = useCallback(
    (_session: AuthenticatedSessionModel, isNew: boolean) => {
      if (isNew) setWelcome(true)
      else navigate('/cliente')
    },
    [navigate],
  )

  const switchMode = (next: Mode) => {
    setMode(next)
    setError(null)
  }

  const handleSubmit = async (event: SubmitEvent) => {
    event.preventDefault()
    setError(null)
    setLoading(true)
    try {
      if (mode === 'signup') {
        const session = await authenticateCustomer.auth({
          name: name.trim(),
          phone,
          password,
        })
        saveCustomerSession(session)
        setWelcome(true)
      } else {
        saveCustomerSession(
          await authenticateCustomer.auth({ phone, password }),
        )
        navigate('/cliente')
      }
    } catch (cause) {
      setError(errorMessage(cause, 'Não foi possível entrar'))
    } finally {
      setLoading(false)
    }
  }

  if (welcome) {
    return <Welcome onContinue={() => navigate('/cliente')} />
  }

  const title = {
    signup: 'Criar uma conta',
    signin: 'Entrar',
    forgot: 'Esqueci a senha',
    whatsapp: 'Entrar com WhatsApp',
  }[mode]
  const whatsappEnabled = Boolean(access && options?.whatsappCode)
  const googleClientId = access ? options?.googleClientId : null

  return (
    <main className="auth-shell">
      <h1 className="auth-brand">
        <BrandLogo size="lg" />
      </h1>
      <p className="muted">Área do cliente</p>

      <section className="auth-card" aria-labelledby="auth-title">
        <h2 id="auth-title">{title}</h2>

        {mode === 'forgot' ? (
          <>
            <p className="subtitle">
              {whatsappEnabled
                ? 'Entre com um código enviado ao seu WhatsApp, sem precisar da senha.'
                : 'Ainda não há recuperação de senha pelo app. Fale com o atendimento do lava-jato para redefinir o seu acesso.'}
            </p>
            {whatsappEnabled && (
              <button
                type="button"
                className="btn btn-dark"
                onClick={() => switchMode('whatsapp')}
              >
                Receber código no WhatsApp
              </button>
            )}
            <div className="auth-links">
              <button type="button" onClick={() => switchMode('signin')}>
                Voltar para entrar
              </button>
            </div>
          </>
        ) : mode === 'whatsapp' && access ? (
          <WhatsAppLogin
            access={access}
            onSession={onPasswordless}
            onCancel={() => switchMode('signin')}
          />
        ) : (
          <form className="form" onSubmit={handleSubmit}>
            {mode === 'signup' && (
              <TextField
                label="Nome"
                value={name}
                onChange={setName}
                autoComplete="name"
              />
            )}
            <PhoneField value={phone} onChange={setPhone} />
            <PasswordField
              label={
                mode === 'signup' ? 'Senha (mínimo 8 caracteres)' : 'Senha'
              }
              value={password}
              onChange={setPassword}
              autoComplete={
                mode === 'signup' ? 'new-password' : 'current-password'
              }
            />
            {mode === 'signup' && (
              <p className="auth-note">
                Ao continuar, sua conta de cliente é criada com este telefone.
              </p>
            )}
            <Notice tone="error">{error}</Notice>
            <button type="submit" className="btn btn-dark" disabled={loading}>
              {loading
                ? 'Aguarde...'
                : mode === 'signup'
                  ? 'Criar uma conta'
                  : 'Entrar'}
            </button>
            <div className="auth-links">
              <button type="button" onClick={() => switchMode('forgot')}>
                Esqueci a senha
              </button>
              {mode === 'signup' ? (
                <button type="button" onClick={() => switchMode('signin')}>
                  Entrar
                </button>
              ) : (
                <button type="button" onClick={() => switchMode('signup')}>
                  Criar conta
                </button>
              )}
            </div>
            {(whatsappEnabled || googleClientId) && access && (
              <>
                <p className="auth-divider">ou</p>
                {whatsappEnabled && (
                  <button
                    type="button"
                    className="btn btn-outline"
                    onClick={() => switchMode('whatsapp')}
                  >
                    <UiIcon name="phone" size={18} />
                    Entrar com WhatsApp (sem senha)
                  </button>
                )}
                {googleClientId && (
                  <GoogleButton
                    access={access}
                    clientId={googleClientId}
                    onSession={onPasswordless}
                    onError={setError}
                  />
                )}
              </>
            )}
          </form>
        )}
      </section>

      <InstallApp />
      <Link to="/" className="auth-back">
        Voltar para a escolha de perfil
      </Link>
    </main>
  )
}

function InstallApp() {
  const { installed, install } = usePwaInstall()
  const [hint, setHint] = useState<string | null>(null)
  if (installed) return null

  return (
    <section className="install-app" aria-label="Instalar o app">
      <p>Para celular ou tablet, instale o app:</p>
      <div className="auth-links">
        <button
          type="button"
          onClick={() =>
            setHint(
              'No Safari, toque em Compartilhar e depois em “Adicionar à Tela de Início”.',
            )
          }
        >
          <UiIcon name="phone" size={16} />
          iPhone e iPad
        </button>
        <button
          type="button"
          onClick={() => {
            void install().then((prompted) => {
              if (!prompted) {
                setHint(
                  'No Chrome, abra o menu ⋮ e toque em “Instalar app” ou “Adicionar à tela inicial”.',
                )
              }
            })
          }}
        >
          <UiIcon name="download" size={16} />
          Android
        </button>
      </div>
      {hint && <p className="faint">{hint}</p>}
    </section>
  )
}

const STEPS = [
  {
    marker: <UiIcon name="car" size={20} />,
    title: 'Cadastre seu veículo',
    text: 'Na aba Veículos, informe a placa, o modelo e a cor.',
  },
  {
    marker: '1',
    title: 'Agende uma lavagem',
    text: 'Escolha o serviço e o horário (segunda a sábado, 8h às 18h).',
  },
  {
    marker: '2',
    title: 'Acompanhe em tempo real',
    text: 'O painel mostra quando a lavagem começa, pausa e termina.',
  },
  {
    marker: <UiIcon name="check" size={20} />,
    title: 'Tudo pronto',
    text: 'Quando o carro estiver pronto, é só passar no balcão para retirar.',
  },
]

function Welcome({ onContinue }: Readonly<{ onContinue: () => void }>) {
  return (
    <main className="auth-shell">
      <BrandLogo size="lg" />
      <section className="welcome" aria-labelledby="welcome-title">
        <h1 id="welcome-title">Bem-vindo ao Washflow!</h1>
        <p className="muted">Confira os primeiros passos:</p>
        <ol className="stepper-steps">
          {STEPS.map((step, index) => (
            <li key={step.title} className="stepper-step">
              <span
                className={`stepper-dot${index === 0 || index === STEPS.length - 1 ? ' is-icon' : ''}`}
                aria-hidden="true"
              >
                {step.marker}
              </span>
              <div>
                <h3>{step.title}</h3>
                <p className="muted">{step.text}</p>
              </div>
            </li>
          ))}
        </ol>
        <button
          type="button"
          className="btn btn-accent btn-block"
          onClick={onContinue}
        >
          Perfeito, entendi, vou começar
        </button>
      </section>
    </main>
  )
}
