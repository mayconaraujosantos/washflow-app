import { SubmitEvent, useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import type { EstablishmentSummaryModel } from '@/domain/models/platform-model'
import type { Platform } from '@/domain/usecases/platform'
import {
  clearAdminSession,
  loadAdminSession,
} from '@/main/session/admin-session'
import { PasswordResetForm } from '@/presentation/components/password-reset-form'
import { ScreenLayout } from '@/presentation/components/ui/app-chrome'
import {
  PasswordField,
  PhoneField,
  TextField,
} from '@/presentation/components/ui/auth-fields'
import { Notice } from '@/presentation/components/ui/blocks'
import { errorMessage } from '@/presentation/helpers/format'
import {
  HoursFields,
  type HoursValue,
} from '@/presentation/pages/setup/hours-fields'

type Props = Readonly<{ platform: Platform }>

const emptyHours: HoursValue = {
  openingHour: 8,
  closingHour: 18,
  openOnSunday: false,
  maxVehiclesPerHour: 3,
}

/** Platform administration: every lava-jato, creation and suspension. */
export function AdminPanel({ platform }: Props) {
  const navigate = useNavigate()
  const [items, setItems] = useState<EstablishmentSummaryModel[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [creating, setCreating] = useState(false)
  const [notice, setNotice] = useState<string | null>(null)
  const [resettingId, setResettingId] = useState<string | null>(null)

  const load = useCallback(() => {
    platform
      .list()
      .then((loaded) => {
        setItems(loaded)
        setError(null)
      })
      .catch((cause) =>
        setError(
          errorMessage(cause, 'Não foi possível carregar os lava-jatos'),
        ),
      )
  }, [platform])

  useEffect(() => {
    if (!loadAdminSession()) {
      navigate('/login/admin', { replace: true })
      return
    }
    load()
  }, [load, navigate])

  const logout = () => {
    clearAdminSession()
    navigate('/login/admin', { replace: true })
  }

  const resetManagerPassword = async (
    item: EstablishmentSummaryModel,
    password: string,
  ) => {
    setError(null)
    setNotice(null)
    try {
      await platform.resetManagerPassword(item.id, password)
      setResettingId(null)
      setNotice(
        `Senha de ${item.managerName ?? 'gerente'} (${item.name}) redefinida. Entregue a nova senha pessoalmente.`,
      )
    } catch (cause) {
      setError(errorMessage(cause, 'Não foi possível redefinir a senha'))
    }
  }

  const toggle = async (item: EstablishmentSummaryModel) => {
    if (
      item.active &&
      !window.confirm(
        `Suspender ${item.name}? A equipe e os clientes deixam de acessar até reativar.`,
      )
    ) {
      return
    }
    try {
      const updated = await platform.setActive(item.id, !item.active)
      setItems((current) =>
        (current ?? []).map((row) => (row.id === updated.id ? updated : row)),
      )
    } catch (cause) {
      setError(errorMessage(cause, 'Não foi possível alterar o lava-jato'))
    }
  }

  return (
    <ScreenLayout
      title="Lava-jatos"
      actions={
        <button type="button" className="link-action" onClick={logout}>
          Sair
        </button>
      }
    >
      <Notice tone="error">{error}</Notice>
      <Notice tone="success">{notice}</Notice>
      {creating ? (
        <NewEstablishmentForm
          platform={platform}
          onCancel={() => setCreating(false)}
          onCreated={(created) => {
            setCreating(false)
            setNotice(
              `${created.name} criado. Entregue ao gerente o e-mail ${created.managerEmail ?? ''} e a senha inicial.`,
            )
            setItems((current) => [...(current ?? []), created])
          }}
        />
      ) : (
        <button
          type="button"
          className="btn btn-accent btn-block"
          onClick={() => {
            setNotice(null)
            setCreating(true)
          }}
        >
          Novo lava-jato
        </button>
      )}

      {items === null && !error && (
        <p className="muted" role="status">
          Carregando...
        </p>
      )}
      {items && items.length === 0 && (
        <p className="muted">Nenhum lava-jato cadastrado ainda.</p>
      )}
      <ul className="admin-list" aria-label="Lava-jatos cadastrados">
        {(items ?? []).map((item) => (
          <li key={item.id} className="admin-card">
            <div className="admin-card-head">
              <strong>{item.name}</strong>
              <span
                className={`status-badge ${item.active ? 'status-active' : 'status-suspended'}`}
              >
                {item.active ? 'Ativo' : 'Suspenso'}
              </span>
            </div>
            <p className="muted">
              Gerente: {item.managerName ?? '-'} · {item.managerEmail ?? '-'}
            </p>
            <p className="admin-link">
              Link dos clientes:{' '}
              <code>{`${window.location.origin}/?lavajato=${item.slug}`}</code>
            </p>
            <div className="auth-links">
              <button
                type="button"
                onClick={() =>
                  void navigator.clipboard
                    ?.writeText(
                      `${window.location.origin}/?lavajato=${item.slug}`,
                    )
                    .then(() => setNotice('Link copiado.'))
                    .catch(() => undefined)
                }
              >
                Copiar link
              </button>
              <button type="button" onClick={() => void toggle(item)}>
                {item.active ? 'Suspender' : 'Reativar'}
              </button>
              {item.managerEmail && (
                <button
                  type="button"
                  onClick={() =>
                    setResettingId((current) =>
                      current === item.id ? null : item.id,
                    )
                  }
                >
                  Redefinir senha do gerente
                </button>
              )}
            </div>
            {resettingId === item.id && (
              <PasswordResetForm
                name={item.managerName ?? item.name}
                onSave={(password) => resetManagerPassword(item, password)}
                onCancel={() => setResettingId(null)}
              />
            )}
          </li>
        ))}
      </ul>
    </ScreenLayout>
  )
}

function NewEstablishmentForm({
  platform,
  onCancel,
  onCreated,
}: Readonly<{
  platform: Platform
  onCancel: () => void
  onCreated: (created: EstablishmentSummaryModel) => void
}>) {
  const [name, setName] = useState('')
  const [hours, setHours] = useState<HoursValue>(emptyHours)
  const [managerName, setManagerName] = useState('')
  const [managerEmail, setManagerEmail] = useState('')
  const [managerPhone, setManagerPhone] = useState('')
  const [managerPassword, setManagerPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  const submit = async (event: SubmitEvent) => {
    event.preventDefault()
    setError(null)
    if (hours.openingHour >= hours.closingHour) {
      setError('A abertura deve ser antes do fechamento.')
      return
    }
    setSaving(true)
    try {
      onCreated(
        await platform.create({
          name: name.trim(),
          ...hours,
          managerName: managerName.trim(),
          managerEmail: managerEmail.trim(),
          managerPhone: managerPhone || undefined,
          managerPassword,
        }),
      )
    } catch (cause) {
      setError(errorMessage(cause, 'Não foi possível criar o lava-jato'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <form
      className="form admin-form"
      onSubmit={submit}
      aria-label="Novo lava-jato"
    >
      <h2>Novo lava-jato</h2>
      <TextField
        label="Nome do lava-jato"
        value={name}
        onChange={setName}
        autoComplete="organization"
      />
      <HoursFields value={hours} onChange={setHours} />
      <h3>Gerente (dono)</h3>
      <TextField
        label="Nome do gerente"
        value={managerName}
        onChange={setManagerName}
        autoComplete="off"
      />
      <TextField
        label="E-mail de acesso do gerente"
        value={managerEmail}
        onChange={setManagerEmail}
        autoComplete="off"
        inputMode="email"
        maxLength={254}
      />
      <PhoneField
        label="Telefone do gerente (opcional)"
        value={managerPhone}
        onChange={setManagerPhone}
        required={false}
      />
      <PasswordField
        label="Senha inicial (mínimo 8 caracteres)"
        value={managerPassword}
        onChange={setManagerPassword}
        autoComplete="new-password"
      />
      <Notice tone="error">{error}</Notice>
      <button
        type="submit"
        className="btn btn-accent btn-block"
        disabled={saving}
      >
        {saving ? 'Criando...' : 'Criar lava-jato'}
      </button>
      <div className="auth-links">
        <button type="button" onClick={onCancel}>
          Cancelar
        </button>
      </div>
    </form>
  )
}
