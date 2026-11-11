import { SubmitEvent, useEffect, useState } from 'react'
import type { UserModel } from '@/domain/models/user-model'
import type { Access } from '@/domain/usecases/access'
import { Notice } from '@/presentation/components/ui/blocks'
import { errorMessage, formatPhone } from '@/presentation/helpers/format'

type Props = Readonly<{
  user: UserModel
  access: Access
  shopName: string
}>

/** "Meus dados": who the account is and the e-mail that receives reset codes. */
export function AccountSection({ user, access, shopName }: Props) {
  const [email, setEmail] = useState('')
  const [saved, setSaved] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    access
      .loadRecoveryEmail(user.id)
      .then((current) => {
        if (!active) return
        setSaved(current)
        setEmail(current ?? '')
      })
      .catch((cause) => {
        if (active) setError(errorMessage(cause, 'Não foi possível carregar'))
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [access, user.id])

  const save = async (event: SubmitEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)
    setNotice(null)
    setSaving(true)
    try {
      const current = await access.saveRecoveryEmail(
        user.id,
        email.trim() || null,
      )
      setSaved(current)
      setEmail(current ?? '')
      setNotice(
        current
          ? `Pronto: se esquecer a senha, enviamos um código para ${current}.`
          : 'E-mail removido. Para trocar a senha, peça no balcão.',
      )
    } catch (cause) {
      setError(errorMessage(cause, 'Não foi possível salvar o e-mail'))
    } finally {
      setSaving(false)
    }
  }

  const unchanged = email.trim().toLowerCase() === (saved ?? '')

  return (
    <section className="form">
      <h2>Meus dados</h2>
      <dl className="account-facts">
        <dt>Nome</dt>
        <dd>{user.name}</dd>
        <dt>Telefone</dt>
        <dd>{user.phone ? formatPhone(user.phone) : '—'}</dd>
        {shopName && (
          <>
            <dt>Lava-jato</dt>
            <dd>{shopName}</dd>
          </>
        )}
      </dl>
      <form className="form" onSubmit={(event) => void save(event)}>
        <label className="field">
          <span>E-mail para recuperar a senha</span>
          <input
            className="input"
            type="email"
            inputMode="email"
            autoComplete="email"
            maxLength={254}
            placeholder="voce@exemplo.com"
            value={email}
            disabled={loading}
            onChange={(event) => setEmail(event.target.value)}
          />
        </label>
        <p className="faint">
          Se esquecer a senha, enviamos para este e-mail um código para criar
          uma nova.
        </p>
        <Notice tone="error">{error}</Notice>
        <Notice tone="success">{notice}</Notice>
        <button
          type="submit"
          className="btn btn-accent"
          disabled={loading || saving || unchanged}
        >
          {saving ? 'Salvando...' : 'Salvar e-mail'}
        </button>
      </form>
    </section>
  )
}
