import { SubmitEvent, useState } from 'react'

const MIN_LENGTH = 8

type Props = Readonly<{
  /** Whose password: "Ana Cliente". */
  name: string
  onSave: (password: string) => Promise<void>
  onCancel: () => void
}>

/**
 * Inline "new password" form for someone who lost theirs: the manager (or the
 * platform administrator) types it here and hands it over in person.
 */
export function PasswordResetForm({ name, onSave, onCancel }: Props) {
  const [password, setPassword] = useState('')
  const [saving, setSaving] = useState(false)
  const short = password.length < MIN_LENGTH

  const submit = async (event: SubmitEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (short) return
    setSaving(true)
    try {
      await onSave(password)
    } finally {
      setSaving(false)
    }
  }

  return (
    <form className="team-controls" onSubmit={(event) => void submit(event)}>
      <label>
        Nova senha (mínimo 8 caracteres)
        <input
          className="input"
          type="password"
          autoComplete="new-password"
          aria-label={`Nova senha de ${name}`}
          minLength={MIN_LENGTH}
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          required
        />
      </label>
      <button className="secondary-btn" disabled={saving || short}>
        {saving ? 'Salvando...' : 'Salvar nova senha'}
      </button>
      <button type="button" className="secondary-btn" onClick={onCancel}>
        Cancelar
      </button>
    </form>
  )
}
