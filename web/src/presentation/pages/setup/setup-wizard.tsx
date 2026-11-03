import { SubmitEvent, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import type { Establishment } from '@/domain/usecases/establishment'
import { saveManagerSession } from '@/main/session/manager-session'
import { AuthLayout } from '@/presentation/components/ui/app-chrome'
import {
  PasswordField,
  PhoneField,
  TextField,
} from '@/presentation/components/ui/auth-fields'
import { Notice } from '@/presentation/components/ui/blocks'
import { errorMessage } from '@/presentation/helpers/format'
import { HoursFields, type HoursValue } from './hours-fields'

type Props = Readonly<{ establishment: Establishment }>

/**
 * First-run wizard (step 1: the car wash, step 2: the owner's manager account).
 * The API accepts it only while the establishment is unconfigured and, in
 * production, with the server's SETUP_CODE.
 */
export function SetupWizard({ establishment }: Props) {
  const navigate = useNavigate()
  const [step, setStep] = useState<1 | 2>(1)
  const [codeRequired, setCodeRequired] = useState(false)
  const [checking, setChecking] = useState(true)
  const [name, setName] = useState('')
  const [hours, setHours] = useState<HoursValue>({
    openingHour: 8,
    closingHour: 18,
    openOnSunday: false,
    maxVehiclesPerHour: 3,
  })
  const [ownerName, setOwnerName] = useState('')
  const [ownerEmail, setOwnerEmail] = useState('')
  const [ownerPhone, setOwnerPhone] = useState('')
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

  const goToOwner = (event: SubmitEvent) => {
    event.preventDefault()
    setError(null)
    if (hours.openingHour >= hours.closingHour) {
      setError('A abertura deve ser antes do fechamento.')
      return
    }
    setStep(2)
  }

  const finish = async (event: SubmitEvent) => {
    event.preventDefault()
    setError(null)
    setSaving(true)
    try {
      const session = await establishment.setup({
        setupCode: codeRequired ? setupCode : undefined,
        establishmentName: name.trim(),
        ...hours,
        ownerName: ownerName.trim(),
        ownerEmail: ownerEmail.trim(),
        ownerPhone: ownerPhone || undefined,
        ownerPassword: password,
      })
      saveManagerSession(session)
      navigate('/gerente', { replace: true })
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

  if (step === 1) {
    return (
      <AuthLayout
        title="Configure seu lava-jato"
        subtitle="Passo 1 de 2 · Dados do estabelecimento e do pátio"
      >
        <form className="form" onSubmit={goToOwner}>
          <TextField
            label="Nome do lava-jato"
            value={name}
            onChange={setName}
            autoComplete="organization"
          />
          <HoursFields value={hours} onChange={setHours} />
          <Notice tone="error">{error}</Notice>
          <button type="submit" className="btn btn-dark">
            Continuar
          </button>
        </form>
      </AuthLayout>
    )
  }

  return (
    <AuthLayout
      title="Sua conta de dono"
      subtitle="Passo 2 de 2 · Você entra no painel com este e-mail e senha"
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
        <PhoneField
          label="Telefone (opcional)"
          value={ownerPhone}
          onChange={setOwnerPhone}
          required={false}
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
          {saving ? 'Configurando...' : 'Concluir e abrir o painel'}
        </button>
        <div className="auth-links">
          <button type="button" onClick={() => setStep(1)}>
            Voltar
          </button>
        </div>
      </form>
    </AuthLayout>
  )
}
