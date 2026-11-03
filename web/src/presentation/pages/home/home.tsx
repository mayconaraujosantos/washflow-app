import { SubmitEvent, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import type { AuthenticatedSessionModel } from '@/domain/models/authenticated-session-model'
import type { AuthenticateCustomer } from '@/domain/usecases/authenticate-customer'
import type { AuthenticateManager } from '@/domain/usecases/authenticate-manager'
import type { AuthenticateWasher } from '@/domain/usecases/authenticate-washer'
import { saveCustomerSession } from '@/main/session/customer-session'
import { saveManagerSession } from '@/main/session/manager-session'
import { saveWasherSession } from '@/main/session/washer-session'
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

type Mode = 'signup' | 'signin' | 'forgot'
type Profile = 'customer' | 'washer' | 'manager'

const PROFILE_LABELS: Record<Profile, string> = {
  customer: 'Cliente',
  washer: 'Lavador',
  manager: 'Gerente',
}

const PANEL_PATHS: Record<Profile, string> = {
  customer: '/cliente',
  washer: '/lavador',
  manager: '/gerente',
}

type Props = Readonly<{
  authenticateCustomer: AuthenticateCustomer
  // Without these the screen is customer-only (the customer QR code page).
  authenticateWasher?: AuthenticateWasher
  authenticateManager?: AuthenticateManager
  initialMode?: Exclude<Mode, 'forgot'>
}>

/**
 * Entry screen: customers create an account (name, phone, password); anyone
 * with an account signs in with phone and password and picks their profile.
 * Staff accounts are provisioned by a manager, so only customers sign up.
 */
export function Home({
  authenticateCustomer,
  authenticateWasher,
  authenticateManager,
  initialMode = 'signup',
}: Props) {
  const navigate = useNavigate()
  const [mode, setMode] = useState<Mode>(initialMode)
  const [profile, setProfile] = useState<Profile>('customer')
  const [name, setName] = useState('')
  const [phone, setPhone] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [welcome, setWelcome] = useState(false)

  const profiles: Profile[] = [
    'customer',
    ...(authenticateWasher ? (['washer'] as const) : []),
    ...(authenticateManager ? (['manager'] as const) : []),
  ]

  const switchMode = (next: Mode) => {
    setMode(next)
    setError(null)
  }

  const signIn = async (): Promise<void> => {
    let session: AuthenticatedSessionModel
    if (profile === 'washer' && authenticateWasher) {
      session = await authenticateWasher.auth({ phone, password })
      saveWasherSession(session)
    } else if (profile === 'manager' && authenticateManager) {
      session = await authenticateManager.auth({ phone, password })
      saveManagerSession(session)
    } else {
      session = await authenticateCustomer.auth({ phone, password })
      saveCustomerSession(session)
    }
    navigate(PANEL_PATHS[profile])
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
        await signIn()
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
  }[mode]

  return (
    <main className="auth-shell">
      <h1 className="auth-brand">
        <BrandLogo size="lg" />
      </h1>

      <section className="auth-card" aria-labelledby="auth-title">
        <h2 id="auth-title">{title}</h2>

        {mode === 'forgot' ? (
          <>
            <p className="subtitle">
              Ainda não há recuperação de senha pelo app, porque o telefone não
              é verificado por SMS. Fale com o atendimento do lava-jato para
              redefinir o seu acesso.
            </p>
            <div className="auth-links">
              <button type="button" onClick={() => switchMode('signin')}>
                Voltar para entrar
              </button>
            </div>
          </>
        ) : (
          <form className="form" onSubmit={handleSubmit}>
            {mode === 'signin' && profiles.length > 1 && (
              <div
                className="segmented segmented-block"
                role="group"
                aria-label="Entrar como"
              >
                {profiles.map((option) => (
                  <button
                    key={option}
                    type="button"
                    aria-pressed={profile === option}
                    onClick={() => setProfile(option)}
                  >
                    {PROFILE_LABELS[option]}
                  </button>
                ))}
              </div>
            )}
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
          </form>
        )}
      </section>

      <InstallApp />
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
