import { SubmitEvent, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import type { Establishment } from '@/domain/usecases/establishment'
import { saveAdminSession } from '@/main/session/admin-session'
import { AuthLayout } from '@/presentation/components/ui/app-chrome'
import {
  PasswordField,
  TextField,
} from '@/presentation/components/ui/auth-fields'
import { Notice } from '@/presentation/components/ui/blocks'
import { errorMessage } from '@/presentation/helpers/format'

type Props = Readonly<{ establishment: Establishment }>

/**
 * First run of the platform: creates the administrator account, who then
 * creates each lava-jato (and its manager) from the admin panel. The API
 * accepts it only once and, in production, with the server's SETUP_CODE.
 */
export function SetupWizard({ establishment }: Props) {
  const navigate = useNavigate()
  const [codeRequired, setCodeRequired] = useState(false)
  const [checking, setChecking] = useState(true)
  const [ownerName, setOwnerName] = useState('')
  const [ownerEmail, setOwnerEmail] = useState('')
  const [password, setPassword] = useState('')
  const [setupCode, setSetupCode] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    let active = true
    establishment
      .setupStatus()
      .then((status) => {
        if (!active) return
        if (status.configured) {
          navigate('/', { replace: true })
          return
        }
        setCodeRequired(status.setupCodeRequired)
        setChecking(false)
      })
      .catch((cause) => {
        if (!active) return
        setError(
          errorMessage(cause, 'Não foi possível verificar a configuração'),
        )
        setChecking(false)
      })
    return () => {
      active = false
    }
  }, [establishment, navigate])

  const finish = async (event: SubmitEvent) => {
    event.preventDefault()
    setError(null)
    setSaving(true)
    try {
      const session = await establishment.setup({
        setupCode: codeRequired ? setupCode : undefined,
        ownerName: ownerName.trim(),
        ownerEmail: ownerEmail.trim(),
        ownerPassword: password,
      })
      saveAdminSession(session)
      navigate('/admin', { replace: true })
    } catch (cause) {
      setError(errorMessage(cause, 'Não foi possível concluir a configuração'))
    } finally {
      setSaving(false)
    }
  }

  if (checking) {
    return (
      <AuthLayout title="Preparando...">
        <Notice tone="error">{error}</Notice>
      </AuthLayout>
    )
  }

  return (
    <AuthLayout
      title="Configure a plataforma"
      subtitle="Crie a conta de administrador. Depois você cadastra cada lava-jato e o gerente dele."
    >
      <form className="form" onSubmit={finish}>
        <TextField
          label="Seu nome"
          value={ownerName}
          onChange={setOwnerName}
          autoComplete="name"
        />
        <TextField
          label="E-mail de acesso"
          value={ownerEmail}
          onChange={setOwnerEmail}
          autoComplete="email"
          inputMode="email"
          maxLength={254}
        />
        <PasswordField
          label="Senha (mínimo 8 caracteres)"
          value={password}
          onChange={setPassword}
          autoComplete="new-password"
        />
        {codeRequired && (
          <>
            <TextField
              label="Código de configuração"
              value={setupCode}
              onChange={setSetupCode}
              autoComplete="one-time-code"
            />
            <p className="auth-note">
              Por segurança, informe o SETUP_CODE definido no servidor.
            </p>
          </>
        )}
        <Notice tone="error">{error}</Notice>
        <button type="submit" className="btn btn-dark" disabled={saving}>
          {saving ? 'Configurando...' : 'Criar administrador'}
        </button>
      </form>
    </AuthLayout>
  )
}
