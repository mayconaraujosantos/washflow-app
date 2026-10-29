import { SubmitEvent, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { UiIcon } from '@/presentation/components/ui-icon'
import type { UserModel, UserProfile } from '@/domain/models/user-model'
import type { Operations } from '@/domain/usecases/operations'
import { clearManagerSession } from '@/main/session/manager-session'

const USER_PROFILE_LABELS: Record<UserProfile, string> = {
  CUSTOMER: 'Cliente',
  WASHER: 'Lavador',
  MANAGER: 'Gerente',
}

const USER_PROFILES: UserProfile[] = ['CUSTOMER', 'WASHER', 'MANAGER']

type Props = Readonly<{
  users: UserModel[]
  currentUser: UserModel
  operations: Operations
  teamLoading: boolean
  teamError: string | null
  busyUserId: string | null
  setBusyUserId: (id: string | null) => void
  setNotice: (msg: string | null) => void
  setError: (msg: string | null) => void
}>

export function TeamManagementSection({
  users,
  currentUser,
  operations,
  teamLoading,
  teamError,
  busyUserId,
  setBusyUserId,
  setNotice,
  setError,
}: Props) {
  const navigate = useNavigate()
  const [selectedProfiles, setSelectedProfiles] = useState<Record<string, UserProfile>>({})
  const [washerName, setWasherName] = useState('')
  const [washerPhone, setWasherPhone] = useState('')
  const [washerPassword, setWasherPassword] = useState('')
  const [creatingWasher, setCreatingWasher] = useState(false)

  const createWasher = async (event: SubmitEvent<HTMLFormElement>) => {
    event.preventDefault()
    setCreatingWasher(true)
    setError(null)
    setNotice(null)
    try {
      await operations.createWasher({
        name: washerName.trim(),
        phone: washerPhone.trim(),
        password: washerPassword,
      })
      // Users will be refreshed by the parent component
      setWasherName('')
      setWasherPhone('')
      setWasherPassword('')
      setNotice(
        'Conta de lavador criada. Entregue a senha inicial diretamente ao colaborador.',
      )
    } catch (cause) {
      setError(
        cause instanceof Error ? cause.message : 'Não foi possível cadastrar o lavador',
      )
    } finally {
      setCreatingWasher(false)
    }
  }

  const changeUserProfile = async (target: UserModel) => {
    const newProfile = selectedProfiles[target.id] ?? target.profile
    if (newProfile === target.profile) return
    setBusyUserId(target.id)
    setError(null)
    setNotice(null)
    try {
      await operations.updateUserProfile(target.id, currentUser.id, newProfile)
      // Users will be refreshed by the parent component
      setSelectedProfiles((current) => {
        const next = { ...current }
        delete next[target.id]
        return next
      })
      if (target.id === currentUser.id) {
        clearManagerSession()
        navigate(
          `/login/${newProfile === 'CUSTOMER' ? 'cliente' : newProfile === 'WASHER' ? 'lavador' : 'gerente'}`,
          { replace: true },
        )
        return
      }
      setNotice(`Perfil de ${target.name} alterado para ${USER_PROFILE_LABELS[newProfile]}.`)
    } catch (cause) {
      setError(
        cause instanceof Error ? cause.message : 'Não foi possível alterar o perfil',
      )
    } finally {
      setBusyUserId(null)
    }
  }

  const toggleUserActive = async (target: UserModel) => {
    setBusyUserId(target.id)
    setError(null)
    setNotice(null)
    try {
      await operations.setUserActive(target.id, !target.active)
      if (target.id === currentUser.id && target.active) {
        clearManagerSession()
        navigate('/', { replace: true })
        return
      }
      // Users will be refreshed by the parent component
      setNotice(
        target.active ? `Acesso de ${target.name} desativado.` : `Acesso de ${target.name} reativado.`,
      )
    } catch (cause) {
      setError(
        cause instanceof Error ? cause.message : 'Não foi possível atualizar o acesso',
      )
    } finally {
      setBusyUserId(null)
    }
  }

  return (
    <section className="dashboard-section team-section">
      <div className="section-header">
        <div>
          <h2 className="section-title">
            <span className="section-icon">
              <UiIcon name="manager" />
            </span>
            Equipe e acessos
          </h2>
          <p className="team-description">
            Cadastre lavadores e gerencie os perfis de acesso.
          </p>
        </div>
        <span className="team-count">{users.length} contas</span>
      </div>

      <form className="washer-create-form" onSubmit={(event) => void createWasher(event)}>
        <label>
          Nome
          <input
            autoComplete="name"
            maxLength={100}
            value={washerName}
            onChange={(event) => setWasherName(event.target.value)}
            required
          />
        </label>
        <label>
          Telefone
          <input
            autoComplete="tel"
            type="tel"
            maxLength={20}
            value={washerPhone}
            onChange={(event) => setWasherPhone(event.target.value)}
            required
          />
        </label>
        <label>
          Senha inicial
          <input
            autoComplete="new-password"
            minLength={8}
            type="password"
            value={washerPassword}
            onChange={(event) => setWasherPassword(event.target.value)}
            required
          />
        </label>
        <button className="primary-btn" disabled={creatingWasher}>
          <UiIcon name="washer" size={16} />
          {creatingWasher ? 'Cadastrando...' : 'Cadastrar lavador'}
        </button>
      </form>

      {teamError && (
        <p className="error-message" role="alert">
          {teamError}
        </p>
      )}
      {teamLoading ? (
        <p className="empty-state">Carregando contas...</p>
      ) : users.length === 0 ? (
        <p className="empty-state">Nenhuma conta cadastrada.</p>
      ) : (
        <ul className="team-list">
          {users.map((account) => (
            <li key={account.id} className={!account.active ? 'account-inactive' : ''}>
              <div className="team-user">
                <span className="team-avatar">
                  <UiIcon
                    name={
                      account.profile === 'WASHER'
                        ? 'washer'
                        : account.profile === 'MANAGER'
                          ? 'manager'
                          : 'car'
                    }
                    size={18}
                  />
                </span>
                <div>
                  <strong>{account.name}</strong>
                  <span>{account.phone}</span>
                </div>
              </div>
              <span className={`account-status ${account.active ? 'is-active' : 'is-inactive'}`}>
                {account.active ? 'Ativo' : 'Acesso desativado'}
              </span>
              <div className="team-controls">
                <label>
                  Perfil
                  <select
                    aria-label={`Perfil de ${account.name}`}
                    value={selectedProfiles[account.id] ?? account.profile}
                    onChange={(event) =>
                      setSelectedProfiles((current) => ({
                        ...current,
                        [account.id]: event.target.value as UserProfile,
                      }))
                    }
                  >
                    {USER_PROFILES.map((profile) => (
                      <option key={profile} value={profile}>
                        {USER_PROFILE_LABELS[profile]}
                      </option>
                    ))}
                  </select>
                </label>
                <button
                  className="secondary-btn"
                  disabled={
                    busyUserId === account.id ||
                    !account.active ||
                    (selectedProfiles[account.id] ?? account.profile) === account.profile
                  }
                  onClick={() => void changeUserProfile(account)}
                >
                  Salvar perfil
                </button>
                <button
                  className="secondary-btn"
                  disabled={busyUserId === account.id}
                  onClick={() => void toggleUserActive(account)}
                >
                  {busyUserId === account.id
                    ? 'Salvando...'
                    : account.active
                      ? 'Desativar acesso'
                      : 'Reativar acesso'}
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
