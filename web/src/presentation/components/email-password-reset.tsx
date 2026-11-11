import { SubmitEvent, useEffect, useState } from 'react'
import type { PasswordResetAccount } from '@/domain/models/access-model'
import type { Access } from '@/domain/usecases/access'
import {
  PasswordField,
  PhoneField,
  TextField,
} from '@/presentation/components/ui/auth-fields'
import { Notice } from '@/presentation/components/ui/blocks'
import { errorMessage } from '@/presentation/helpers/format'

const RESEND_AFTER_SECONDS = 60

type Props = Readonly<{
  access: Access
  /** Customers identify themselves by phone, managers by their login e-mail. */
  kind: PasswordResetAccount['kind']
  initialAccount?: string
  /** Called with the new password once it is saved (e.g. to sign in right away). */
  onDone: (account: string, password: string) => Promise<void> | void
  onCancel: () => void
}>

/**
 * "Esqueci a senha" by e-mail: ask for the phone (customer) or e-mail
 * (manager), send a 6-digit code, then set the new password with it.
 */
export function EmailPasswordReset({
  access,
  kind,
  initialAccount = '',
  onDone,
  onCancel,
}: Props) {
  const [account, setAccount] = useState(initialAccount)
  const [step, setStep] = useState<'ask' | 'confirm'>('ask')
  const [code, setCode] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [resendIn, setResendIn] = useState(0)

  useEffect(() => {
    if (resendIn <= 0) return
    const timer = window.setTimeout(() => setResendIn((s) => s - 1), 1000)
    return () => window.clearTimeout(timer)
  }, [resendIn])

  const target = (): PasswordResetAccount =>
    kind === 'customer'
      ? { kind, phone: account }
      : { kind, email: account.trim() }

  const send = async () => {
    setError(null)
    setBusy(true)
    try {
      await access.requestPasswordReset(target())
      setStep('confirm')
      setResendIn(RESEND_AFTER_SECONDS)
    } catch (cause) {
      setError(errorMessage(cause, 'Não foi possível enviar o código'))
    } finally {
      setBusy(false)
    }
  }

  const submitAccount = (event: SubmitEvent<HTMLFormElement>) => {
    event.preventDefault()
    void send()
  }

  const submitCode = async (event: SubmitEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)
    setBusy(true)
    try {
      await access.confirmPasswordReset(target(), code.trim(), password)
      await onDone(account, password)
    } catch (cause) {
      setError(errorMessage(cause, 'Não foi possível trocar a senha'))
      setBusy(false)
    }
  }

  if (step === 'ask') {
    return (
      <form className="form" onSubmit={submitAccount}>
        <p className="subtitle">
          {kind === 'customer'
            ? 'Informe seu telefone: enviamos um código para o e-mail cadastrado em "Meus dados".'
            : 'Informe seu e-mail de acesso: enviamos um código para ele.'}
        </p>
        {kind === 'customer' ? (
          <PhoneField value={account} onChange={setAccount} />
        ) : (
          <TextField
            label="E-mail de acesso"
            value={account}
            onChange={setAccount}
            autoComplete="username"
            inputMode="email"
            maxLength={254}
          />
        )}
        <Notice tone="error">{error}</Notice>
        <button type="submit" className="btn btn-dark" disabled={busy}>
          {busy ? 'Enviando...' : 'Enviar código por e-mail'}
        </button>
        {kind === 'customer' && (
          <p className="auth-note">
            Sem e-mail cadastrado? Peça uma nova senha no balcão do lava-jato.
          </p>
        )}
        <div className="auth-links">
          <button type="button" onClick={onCancel}>
            Voltar para entrar
          </button>
        </div>
      </form>
    )
  }

  return (
    <form className="form" onSubmit={(event) => void submitCode(event)}>
      <p className="subtitle" role="status">
        Se a conta tiver e-mail cadastrado, enviamos um código de 6 dígitos. Ele
        vale por 10 minutos.
      </p>
      <div className="field">
        <label htmlFor="reset-code" className="visually-hidden">
          Código do e-mail
        </label>
        <input
          id="reset-code"
          className="input pin-input"
          inputMode="numeric"
          autoComplete="one-time-code"
          pattern="\d{6}"
          maxLength={6}
          placeholder="Código do e-mail"
          value={code}
          onChange={(event) =>
            setCode(event.target.value.replace(/\D/g, '').slice(0, 6))
          }
          required
        />
      </div>
      <PasswordField
        label="Nova senha (mínimo 8 caracteres)"
        value={password}
        onChange={setPassword}
        autoComplete="new-password"
      />
      <Notice tone="error">{error}</Notice>
      <button
        type="submit"
        className="btn btn-dark"
        disabled={busy || code.length !== 6 || password.length < 8}
      >
        {busy ? 'Salvando...' : 'Trocar senha'}
      </button>
      <div className="auth-links">
        <button
          type="button"
          disabled={busy || resendIn > 0}
          onClick={() => void send()}
        >
          {resendIn > 0 ? `Reenviar código (${resendIn}s)` : 'Reenviar código'}
        </button>
        <button type="button" onClick={onCancel}>
          Voltar para entrar
        </button>
      </div>
    </form>
  )
}
