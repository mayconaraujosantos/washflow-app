import { SubmitEvent, useCallback, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import type { AuthOptionsModel } from '@/domain/models/access-model'
import type { AuthenticatedSessionModel } from '@/domain/models/authenticated-session-model'
import type { Access } from '@/domain/usecases/access'
import type { AuthenticateCustomer } from '@/domain/usecases/authenticate-customer'
import type { EstablishmentDirectory } from '@/domain/usecases/platform'
import { saveCustomerSession } from '@/main/session/customer-session'
import {
  clearEstablishmentChoice,
  loadEstablishmentChoice,
  saveEstablishmentChoice,
  type EstablishmentChoice,
} from '@/main/session/establishment-choice'
import { EmailPasswordReset } from '@/presentation/components/email-password-reset'
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
import { EstablishmentPicker } from './establishment-picker'
import { CodeLogin, GoogleButton } from './passwordless'

type Mode = 'signup' | 'signin' | 'forgot' | 'whatsapp' | 'email'

const EMAIL_SHAPE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

type Props = Readonly<{
  authenticateCustomer: AuthenticateCustomer
  // WhatsApp code / Google sign-in, shown when the API enables them.
  access?: Access
  initialMode?: 'signup' | 'signin'
  // On the home page: links for washers and managers under the card.
  staffLinks?: boolean
  // Customer accounts belong to one lava-jato: with a directory, the customer
  // picks it first (or arrives with it from the shop's link).
  directory?: EstablishmentDirectory
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
  staffLinks = false,
  directory,
}: Props) {
  const navigate = useNavigate()
  const [chosen, setChosen] = useState<EstablishmentChoice | null>(() =>
    directory ? loadEstablishmentChoice() : null,
  )
  const [mode, setMode] = useState<Mode>(initialMode)
  const [name, setName] = useState('')
  const [phone, setPhone] = useState('')
  const [password, setPassword] = useState('')
  const [email, setEmail] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [welcome, setWelcome] = useState(false)
  const [options, setOptions] = useState<AuthOptionsModel | null>(null)

  // A shop link only carries the address: confirm it and learn the name.
  const pendingSlug = chosen && !chosen.name ? chosen.slug : null
  useEffect(() => {
    if (!directory || !pendingSlug) return
    let active = true
    directory
      .search(pendingSlug)
      .then((found) => {
        if (!active) return
        const match = found.find((item) => item.slug === pendingSlug)
        if (match) {
          const confirmed = { slug: match.slug, name: match.name }
          saveEstablishmentChoice(confirmed)
          setChosen(confirmed)
        } else {
          clearEstablishmentChoice()
          setChosen(null)
        }
      })
      .catch(() => {
        // Offline: keep the address; requests will report the problem.
      })
    return () => {
      active = false
    }
  }, [directory, pendingSlug])

  const pick = (choice: EstablishmentChoice) => {
    saveEstablishmentChoice(choice)
    setChosen(choice)
    setError(null)
  }

  const changeEstablishment = () => {
    clearEstablishmentChoice()
    setChosen(null)
  }

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
        const recoveryEmail = email.trim()
        if (recoveryEmail && !EMAIL_SHAPE.test(recoveryEmail)) {
          throw new Error('E-mail inválido')
        }
        const session = await authenticateCustomer.auth({
          name: name.trim(),
          phone,
          password,
        })
        saveCustomerSession(session)
        if (recoveryEmail && access) {
          // The account already exists: a failure here is fixed in "Meus dados".
          await access
            .saveRecoveryEmail(session.user.id, recoveryEmail, session.token)
            .catch(() => undefined)
        }
        setWelcome(true)
      } else {
        saveCustomerSession(
          await authenticateCustomer.auth({ phone, password }),
        )
        navigate('/cliente')
      }
    } catch (cause) {
      const message = errorMessage(cause, 'Não foi possível entrar')
      if (directory && message === 'Lava-jato não encontrado') {
        // The remembered lava-jato was suspended or removed: pick again.
        changeEstablishment()
      }
      setError(message)
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
    email: 'Entrar com e-mail',
  }[mode]
  const whatsappEnabled = Boolean(access && options?.whatsappCode)
  const emailEnabled = Boolean(access && options?.emailCode)
  const googleClientId = access ? options?.googleClientId : null
  const staff = staffLinks && (
    <p className="auth-staff">
      É da equipe? <Link to="/login/lavador">Entrar como lavador</Link>
      {' · '}
      <Link to="/login/gerente">Entrar como gerente</Link>
    </p>
  )

  if (directory && !chosen) {
    return (
      <main className="auth-shell">
        <h1 className="auth-brand">
          <BrandLogo size="lg" />
        </h1>
        <EstablishmentPicker directory={directory} onPick={pick} />
        <InstallApp />
        {staff}
      </main>
    )
  }

  return (
    <main className="auth-shell">
      <h1 className="auth-brand">
        <BrandLogo size="lg" />
      </h1>

      <section className="auth-card" aria-labelledby="auth-title">
        <h2 id="auth-title">{title}</h2>
        {chosen && (
          <p className="auth-establishment">
            <span>
              Lava-jato: <strong>{chosen.name || chosen.slug}</strong>
            </span>
            <button
              type="button"
              className="link-action"
              onClick={changeEstablishment}
            >
              Trocar
            </button>
          </p>
        )}

        {mode === 'forgot' && access && options?.emailCode ? (
          <EmailPasswordReset
            access={access}
            kind="customer"
            initialAccount={phone}
            onDone={async (resetPhone, newPassword) => {
              saveCustomerSession(
                await authenticateCustomer.auth({
                  phone: resetPhone,
                  password: newPassword,
                }),
              )
              navigate('/cliente')
            }}
            onCancel={() => switchMode('signin')}
          />
        ) : mode === 'forgot' ? (
          <>
            <p className="subtitle">
              {whatsappEnabled || emailEnabled
                ? `Entre com um código enviado ao seu ${whatsappEnabled ? 'WhatsApp' : 'e-mail'}${whatsappEnabled && emailEnabled ? ' ou e-mail' : ''}, sem precisar da senha.`
                : 'Peça uma nova senha no balcão: o gerente do lava-jato cria na hora e você entra com ela.'}
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
            {emailEnabled && (
              <button
                type="button"
                className={whatsappEnabled ? 'btn btn-outline' : 'btn btn-dark'}
                onClick={() => switchMode('email')}
              >
                Receber código por e-mail
              </button>
            )}
            <div className="auth-links">
              <button type="button" onClick={() => switchMode('signin')}>
                Voltar para entrar
              </button>
            </div>
          </>
        ) : (mode === 'whatsapp' || mode === 'email') && access ? (
          <CodeLogin
            key={mode}
            access={access}
            channel={mode}
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
              <TextField
                label="E-mail (opcional, para recuperar a senha)"
                value={email}
                onChange={setEmail}
                autoComplete="email"
                inputMode="email"
                maxLength={254}
                required={false}
              />
            )}
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
            {(whatsappEnabled || emailEnabled || googleClientId) && access && (
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
                {emailEnabled && (
                  <button
                    type="button"
                    className="btn btn-outline"
                    onClick={() => switchMode('email')}
                  >
                    <UiIcon name="mail" size={18} />
                    Entrar com e-mail (sem senha)
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
      {staff}
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
    text: 'Escolha os serviços e um horário livre do seu lava-jato.',
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
