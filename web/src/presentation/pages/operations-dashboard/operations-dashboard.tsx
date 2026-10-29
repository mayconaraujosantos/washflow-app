import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { OperationsReportModel } from '@/domain/models/operations-report-model'
import { ServiceOrderModel } from '@/domain/models/service-order-model'
import { ServicePriceModel } from '@/domain/models/service-price-model'
import { UserModel, UserProfile } from '@/domain/models/user-model'
import type { ChecklistType } from '@/domain/models/feature-models'
import {
  Operations,
  PauseReason,
  PaymentMethod,
} from '@/domain/usecases/operations'
import {
  clearManagerSession,
  loadManagerSession,
} from '@/main/session/manager-session'
import {
  clearWasherSession,
  loadWasherSession,
} from '@/main/session/washer-session'
import { BUSINESS_TIME_ZONE } from '@/main/config/business-time'
import { operationsReportCsv } from './operations-report-csv'
import { UiIcon } from '@/presentation/components/ui-icon'
import type { Features } from '@/domain/usecases/features'
import { ChecklistModal } from '@/presentation/components/checklist-modal'
import { CashManager } from '@/presentation/components/cash-manager'
import './operations-dashboard.css'

type Props = Readonly<{
  role: 'washer' | 'manager'
  operations: Operations
  features: Features
}>

const STATUS_LABELS: Record<ServiceOrderModel['status'], string> = {
  SCHEDULED: 'Agendado',
  WAITING_IN_YARD: 'Aguardando no pátio',
  WASHING: 'Em lavagem',
  PAUSED: 'Pausada',
  READY: 'Pronto',
  DONE: 'Finalizado',
}

const PAYMENT_METHODS: { value: PaymentMethod; label: string }[] = [
  { value: 'CASH', label: 'Dinheiro' },
  { value: 'PIX', label: 'Pix' },
  { value: 'DEBIT_CARD', label: 'Cartão de débito' },
  { value: 'CREDIT_CARD', label: 'Cartão de crédito' },
]

const PAUSE_REASONS: { value: PauseReason; label: string }[] = [
  { value: 'LUNCH', label: 'Almoço' },
  { value: 'RESTROOM', label: 'Banheiro' },
  { value: 'OTHER', label: 'Outro motivo' },
]

const PAYMENT_LABELS: Record<string, string> = {
  CASH: 'Dinheiro',
  PIX: 'Pix',
  DEBIT_CARD: 'Cartão de débito',
  CREDIT_CARD: 'Cartão de crédito',
}

const PAUSE_LABELS: Record<PauseReason, string> = {
  LUNCH: 'Almoço',
  RESTROOM: 'Banheiro',
  OTHER: 'Outro',
}

const USER_PROFILE_LABELS: Record<UserProfile, string> = {
  CUSTOMER: 'Cliente',
  WASHER: 'Lavador',
  MANAGER: 'Gerente',
}

const USER_PROFILES: UserProfile[] = ['CUSTOMER', 'WASHER', 'MANAGER']

function currentManausDate(): string {
  const parts = Object.fromEntries(
    new Intl.DateTimeFormat('en', {
      timeZone: BUSINESS_TIME_ZONE,
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    })
      .formatToParts(new Date())
      .map(({ type, value }) => [type, value]),
  )
  return `${parts.year}-${parts.month}-${parts.day}`
}

function downloadReport(report: OperationsReportModel): void {
  const blob = new Blob([`\uFEFF${operationsReportCsv(report)}`], {
    type: 'text/csv;charset=utf-8',
  })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `washflow-relatorio-${report.from}-${report.to}.csv`
  link.click()
  URL.revokeObjectURL(url)
}

export function OperationsDashboard({ role, operations, features }: Props) {
  const navigate = useNavigate()
  const [session] = useState(() =>
    role === 'washer' ? loadWasherSession() : loadManagerSession(),
  )
  const user = session?.user
  const [waiting, setWaiting] = useState<ServiceOrderModel[]>([])
  const [myOrders, setMyOrders] = useState<ServiceOrderModel[]>([])
  const [yard, setYard] = useState<ServiceOrderModel[]>([])
  const [servicePrices, setServicePrices] = useState<ServicePriceModel[]>([])
  const [users, setUsers] = useState<UserModel[]>([])
  const [teamLoading, setTeamLoading] = useState(role === 'manager')
  const [teamError, setTeamError] = useState<string | null>(null)
  const [checklistModal, setChecklistModal] = useState<{
    serviceOrderId: string
    type: ChecklistType
  } | null>(null)
  const [selectedProfiles, setSelectedProfiles] = useState<
    Record<string, UserProfile>
  >({})
  const [washerName, setWasherName] = useState('')
  const [washerPhone, setWasherPhone] = useState('')
  const [washerPassword, setWasherPassword] = useState('')
  const [creatingWasher, setCreatingWasher] = useState(false)
  const [busyUserId, setBusyUserId] = useState<string | null>(null)
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('PIX')
  const [pauseReason, setPauseReason] = useState<PauseReason>('LUNCH')
  const [pauseDescription, setPauseDescription] = useState('')
  const [report, setReport] = useState<OperationsReportModel | null>(null)
  const [reportLoading, setReportLoading] = useState(false)
  const [reportError, setReportError] = useState<string | null>(null)
  const [reportFrom, setReportFrom] = useState(() => {
    const today = currentManausDate()
    return `${today.slice(0, 7)}-01`
  })
  const [reportTo, setReportTo] = useState(currentManausDate)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [busyOrderId, setBusyOrderId] = useState<string | null>(null)

  const refresh = useCallback(async () => {
    try {
      if (role === 'washer') {
        const [queue, active] = await Promise.all([
          operations.loadQueue(),
          operations.loadMyOrders(),
        ])
        setWaiting(queue)
        setMyOrders(active)
      } else {
        const [orders, prices] = await Promise.all([
          operations.loadYard(),
          operations.loadServicePrices(),
        ])
        setYard(orders)
        setServicePrices(prices)
      }
      setError(null)
    } catch (cause) {
      setError(
        cause instanceof Error ? cause.message : 'Falha ao atualizar o painel',
      )
    }
  }, [role, operations])

  useEffect(() => {
    if (!user) {
      navigate(`/login/${role === 'washer' ? 'lavador' : 'gerente'}`, {
        replace: true,
      })
      return
    }
    const initialRefresh = window.setTimeout(() => void refresh(), 0)
    const timer = window.setInterval(() => void refresh(), 3000)
    return () => {
      window.clearTimeout(initialRefresh)
      window.clearInterval(timer)
    }
  }, [user, role, navigate, refresh])

  useEffect(() => {
    if (role !== 'manager' || !user) return
    operations
      .loadUsers()
      .then(setUsers)
      .catch((cause) =>
        setTeamError(
          cause instanceof Error
            ? cause.message
            : 'Não foi possível carregar as contas',
        ),
      )
      .finally(() => setTeamLoading(false))
  }, [operations, role, user])

  const createWasher = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setCreatingWasher(true)
    setError(null)
    setNotice(null)
    try {
      const created = await operations.createWasher({
        name: washerName.trim(),
        phone: washerPhone.trim(),
        password: washerPassword,
      })
      setUsers((current) =>
        [...current, created].sort((a, b) => a.name.localeCompare(b.name)),
      )
      setWasherName('')
      setWasherPhone('')
      setWasherPassword('')
      setNotice(
        'Conta de lavador criada. Entregue a senha inicial diretamente ao colaborador.',
      )
    } catch (cause) {
      setError(
        cause instanceof Error
          ? cause.message
          : 'Não foi possível cadastrar o lavador',
      )
    } finally {
      setCreatingWasher(false)
    }
  }

  const changeUserProfile = async (target: UserModel) => {
    const newProfile = selectedProfiles[target.id] ?? target.profile
    if (newProfile === target.profile || !user) return
    setBusyUserId(target.id)
    setError(null)
    setNotice(null)
    try {
      const updated = await operations.updateUserProfile(
        target.id,
        user.id,
        newProfile,
      )
      setUsers((current) =>
        current.map((account) =>
          account.id === updated.id ? updated : account,
        ),
      )
      setSelectedProfiles((current) => {
        const next = { ...current }
        delete next[target.id]
        return next
      })
      if (target.id === user.id) {
        clearManagerSession()
        navigate(
          `/login/${newProfile === 'CUSTOMER' ? 'cliente' : newProfile === 'WASHER' ? 'lavador' : 'gerente'}`,
          { replace: true },
        )
        return
      }
      setNotice(
        `Perfil de ${target.name} alterado para ${USER_PROFILE_LABELS[newProfile]}.`,
      )
    } catch (cause) {
      setError(
        cause instanceof Error
          ? cause.message
          : 'Não foi possível alterar o perfil',
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
      if (target.id === user?.id && target.active) {
        clearManagerSession()
        navigate('/', { replace: true })
        return
      }
      setUsers((current) =>
        current.map((account) =>
          account.id === target.id
            ? { ...account, active: !target.active }
            : account,
        ),
      )
      setNotice(
        target.active
          ? `Acesso de ${target.name} desativado.`
          : `Acesso de ${target.name} reativado.`,
      )
    } catch (cause) {
      setError(
        cause instanceof Error
          ? cause.message
          : 'Não foi possível atualizar o acesso',
      )
    } finally {
      setBusyUserId(null)
    }
  }

  const generateReport = async () => {
    setReportLoading(true)
    setReportError(null)
    setReport(null)
    try {
      setReport(await operations.loadReport(reportFrom, reportTo))
    } catch (cause) {
      setReportError(
        cause instanceof Error
          ? cause.message
          : 'Falha ao carregar o relatório',
      )
    } finally {
      setReportLoading(false)
    }
  }

  const runAction = async (
    orderId: string,
    action: () => Promise<void>,
    successMessage: string,
  ) => {
    setBusyOrderId(orderId)
    setError(null)
    setNotice(null)
    try {
      await action()
      setNotice(successMessage)
      await refresh()
    } catch (cause) {
      setError(
        cause instanceof Error
          ? cause.message
          : 'Falha ao atualizar atendimento',
      )
    } finally {
      setBusyOrderId(null)
    }
  }

  const logout = () => {
    if (role === 'washer') {
      clearWasherSession()
      navigate('/login/lavador')
    } else {
      clearManagerSession()
      navigate('/login/gerente')
    }
  }

  const renderOrder = (
    order: ServiceOrderModel,
    detail?: React.ReactNode,
    action?: React.ReactNode,
  ) => (
    <li key={order.id}>
      <span className={`status-badge status-${order.status.toLowerCase()}`}>
        {STATUS_LABELS[order.status]}
      </span>
      <span className="order-plate">OS {order.id.slice(0, 8)}</span>
      <span className="order-date">
        {new Date(order.scheduledAt).toLocaleString('pt-BR', {
          timeZone: BUSINESS_TIME_ZONE,
        })}
      </span>
      {order.status === 'PAUSED' && order.pauseReason && (
        <span className="pause-reason">
          Motivo: {PAUSE_LABELS[order.pauseReason]}
        </span>
      )}
      {detail}
      {action}
    </li>
  )

  return (
    <main className="dashboard-shell">
      <div
        className={`dashboard-container${role === 'manager' ? ' dashboard-container-manager' : ''}`}
      >
        <header className="dashboard-header">
          <div>
            <span className="badge">
              <UiIcon
                name={role === 'washer' ? 'washer' : 'manager'}
                size={15}
              />
              Washflow · {role === 'washer' ? 'Lavador' : 'Gerente'}
            </span>
            <h1>Olá, {user?.name.split(' ')[0]}</h1>
            <p className="dashboard-subtitle">
              {role === 'washer'
                ? 'Acompanhe a fila e organize suas lavagens.'
                : 'Visão geral da operação e dos atendimentos.'}
            </p>
          </div>
          <button className="secondary-btn" onClick={logout}>
            <UiIcon name="logout" size={16} />
            Sair
          </button>
        </header>

        {role === 'washer' ? (
          <div className="dashboard-metrics">
            <article>
              <span className="metric-icon metric-icon-blue">
                <UiIcon name="clipboard" />
              </span>
              <div>
                <span>Na fila</span>
                <strong>{waiting.length}</strong>
              </div>
            </article>
            <article>
              <span className="metric-icon metric-icon-violet">
                <UiIcon name="washer" />
              </span>
              <div>
                <span>Minhas lavagens</span>
                <strong>{myOrders.length}</strong>
              </div>
            </article>
            <article>
              <span className="metric-icon metric-icon-amber">
                <UiIcon name="pause" />
              </span>
              <div>
                <span>Pausadas</span>
                <strong>
                  {myOrders.filter((order) => order.status === 'PAUSED').length}
                </strong>
              </div>
            </article>
          </div>
        ) : (
          <div className="dashboard-metrics">
            <article>
              <span className="metric-icon metric-icon-blue">
                <UiIcon name="clipboard" />
              </span>
              <div>
                <span>No pátio</span>
                <strong>{yard.length}</strong>
              </div>
            </article>
            <article>
              <span className="metric-icon metric-icon-violet">
                <UiIcon name="washer" />
              </span>
              <div>
                <span>Em lavagem</span>
                <strong>
                  {yard.filter((order) => order.status === 'WASHING').length}
                </strong>
              </div>
            </article>
            <article>
              <span className="metric-icon metric-icon-green">
                <UiIcon name="check" />
              </span>
              <div>
                <span>Aguardando retirada</span>
                <strong>
                  {yard.filter((order) => order.status === 'READY').length}
                </strong>
              </div>
            </article>
          </div>
        )}

        {error && (
          <p className="error-message" role="alert">
            {error}
          </p>
        )}
        {notice && (
          <p className="success-message" role="status">
            {notice}
          </p>
        )}

        {role === 'washer' ? (
          <>
            <section className="dashboard-section">
              <h2 className="section-title">
                <span className="section-icon">
                  <UiIcon name="clipboard" />
                </span>
                Fila do pátio
              </h2>
              {waiting.length === 0 ? (
                <p className="empty-state">Nenhum veículo aguardando.</p>
              ) : (
                <ul className="order-list">
                  {waiting.map((order) =>
                    renderOrder(
                      order,
                      <button
                        className="primary-btn"
                        disabled={busyOrderId === order.id}
                        onClick={() =>
                          void runAction(
                            order.id,
                            () => operations.start(order.id, user!.id),
                            'Lavagem iniciada.',
                          )
                        }
                      >
                        Iniciar lavagem
                      </button>,
                    ),
                  )}
                </ul>
              )}
            </section>
            <section className="dashboard-section">
              <h2 className="section-title">
                <span className="section-icon">
                  <UiIcon name="washer" />
                </span>
                Minhas lavagens
              </h2>
              {myOrders.length === 0 ? (
                <p className="empty-state">Nenhuma lavagem em andamento.</p>
              ) : (
                <ul className="order-list">
                  {myOrders.map((order) =>
                    renderOrder(
                      order,
                      order.status === 'WASHING' ? (
                        <div className="washer-order-controls">
                          <label>
                            Motivo da pausa
                            <select
                              value={pauseReason}
                              onChange={(event) => {
                                const selectedReason = PAUSE_REASONS.find(
                                  (reason) =>
                                    reason.value === event.target.value,
                                )
                                if (selectedReason) {
                                  setPauseReason(selectedReason.value)
                                }
                              }}
                            >
                              {PAUSE_REASONS.map((reason) => (
                                <option key={reason.value} value={reason.value}>
                                  {reason.label}
                                </option>
                              ))}
                            </select>
                          </label>
                          {pauseReason === 'OTHER' && (
                            <label>
                              Descreva o motivo
                              <input
                                maxLength={200}
                                value={pauseDescription}
                                onChange={(event) =>
                                  setPauseDescription(event.target.value)
                                }
                              />
                            </label>
                          )}
                          <button
                            className="secondary-btn"
                            disabled={
                              busyOrderId === order.id ||
                              (pauseReason === 'OTHER' &&
                                !pauseDescription.trim())
                            }
                            onClick={() =>
                              void runAction(
                                order.id,
                                () =>
                                  operations.pause(
                                    order.id,
                                    pauseReason,
                                    pauseReason === 'OTHER'
                                      ? pauseDescription.trim()
                                      : undefined,
                                  ),
                                `Lavagem pausada: ${PAUSE_LABELS[pauseReason]}.`,
                              )
                            }
                          >
                            <UiIcon name="pause" size={16} />
                            Pausar lavagem
                          </button>
                          <button
                            className="primary-btn"
                            disabled={busyOrderId === order.id}
                            onClick={() =>
                              void runAction(
                                order.id,
                                () => operations.complete(order.id),
                                'Lavagem concluída.',
                              )
                            }
                          >
                            <UiIcon name="check" size={16} />
                            Concluir lavagem
                          </button>
                        </div>
                      ) : (
                        <button
                          className="primary-btn"
                          disabled={busyOrderId === order.id}
                          onClick={() =>
                            void runAction(
                              order.id,
                              () => operations.resume(order.id),
                              'Lavagem retomada.',
                            )
                          }
                        >
                          <UiIcon name="play" size={16} />
                          Retomar lavagem
                        </button>
                      ),
                    ),
                  )}
                </ul>
              )}
            </section>
          </>
        ) : (
          <>
            <section className="dashboard-section">
              <div className="section-header">
                <h2 className="section-title">
                  <span className="section-icon">
                    <UiIcon name="clipboard" />
                  </span>
                  Pátio geral
                </h2>
                <label>
                  <span>Forma de pagamento ao finalizar</span>
                  <select
                    value={paymentMethod}
                    onChange={(event) =>
                      setPaymentMethod(event.target.value as PaymentMethod)
                    }
                  >
                    {PAYMENT_METHODS.map((method) => (
                      <option key={method.value} value={method.value}>
                        {method.label}
                      </option>
                    ))}
                  </select>
                </label>
              </div>
              {yard.length === 0 ? (
                <p className="empty-state">
                  Nenhum atendimento ativo no pátio.
                </p>
              ) : (
                <ul className="order-list">
                  {yard.map((order) =>
                    renderOrder(
                      order,
                      order.status === 'READY' && (
                        <span>
                          Valor: R${' '}
                          {servicePrices
                            .find((price) => price.id === order.servicePriceId)
                            ?.price.toFixed(2) ?? '—'}
                        </span>
                      ),
                      order.status === 'SCHEDULED' ? (
                        <button
                          className="primary-btn"
                          disabled={busyOrderId === order.id}
                          onClick={() =>
                            void runAction(
                              order.id,
                              () => operations.checkIn(order.id),
                              'Veículo recebido no pátio.',
                            )
                          }
                        >
                          <UiIcon name="check" size={16} />
                          Receber no pátio
                        </button>
                      ) : order.status === 'READY' ? (
                        <button
                          className="primary-btn"
                          disabled={busyOrderId === order.id}
                          onClick={() =>
                            void runAction(
                              order.id,
                              () =>
                                operations.finalize(order.id, paymentMethod),
                              'Pagamento registrado e atendimento finalizado.',
                            )
                          }
                        >
                          <UiIcon name="check" size={16} />
                          Registrar pagamento e finalizar
                        </button>
                      ) : undefined,
                    ),
                  )}
                </ul>
              )}
            </section>
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

              <form
                className="washer-create-form"
                onSubmit={(event) => void createWasher(event)}
              >
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
                    <li
                      key={account.id}
                      className={!account.active ? 'account-inactive' : ''}
                    >
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
                      <span
                        className={`account-status ${account.active ? 'is-active' : 'is-inactive'}`}
                      >
                        {account.active ? 'Ativo' : 'Acesso desativado'}
                      </span>
                      <div className="team-controls">
                        <label>
                          Perfil
                          <select
                            aria-label={`Perfil de ${account.name}`}
                            value={
                              selectedProfiles[account.id] ?? account.profile
                            }
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
                            (selectedProfiles[account.id] ??
                              account.profile) === account.profile
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
          </>
        )}
        {role === 'manager' && (
          <section className="dashboard-section report-section">
            <div className="section-header">
              <div>
                <h2 className="section-title">
                  <span className="section-icon">
                    <UiIcon name="chart" />
                  </span>
                  Relatório operacional
                </h2>
                <p>Atendimentos finalizados no período, horário de Manaus.</p>
              </div>
              <div className="report-actions">
                <button
                  type="button"
                  className="primary-btn"
                  disabled={reportLoading}
                  onClick={() => void generateReport()}
                >
                  <UiIcon name="chart" size={16} />
                  {reportLoading ? 'Gerando...' : 'Gerar relatório'}
                </button>
                <button
                  type="button"
                  className="secondary-btn"
                  disabled={!report || reportLoading}
                  onClick={() => report && downloadReport(report)}
                >
                  <UiIcon name="download" size={16} />
                  Exportar CSV
                </button>
              </div>
            </div>
            <div className="report-filters">
              <label>
                De
                <input
                  type="date"
                  value={reportFrom}
                  max={reportTo}
                  onChange={(event) => {
                    setReport(null)
                    setReportFrom(event.target.value)
                  }}
                />
              </label>
              <label>
                Até
                <input
                  type="date"
                  value={reportTo}
                  min={reportFrom}
                  onChange={(event) => {
                    setReport(null)
                    setReportTo(event.target.value)
                  }}
                />
              </label>
            </div>
            {reportError && (
              <p className="error-message" role="alert">
                {reportError}
              </p>
            )}
            {report && (
              <>
                <div className="report-metrics">
                  <article>
                    <span>Atendimentos finalizados</span>
                    <strong>{report.finalizedOrders}</strong>
                  </article>
                  <article>
                    <span>Faturamento</span>
                    <strong>
                      {report.revenue.toLocaleString('pt-BR', {
                        style: 'currency',
                        currency: 'BRL',
                      })}
                    </strong>
                  </article>
                  <article>
                    <span>Comissões de lavadores</span>
                    <strong>
                      {report.commissions.toLocaleString('pt-BR', {
                        style: 'currency',
                        currency: 'BRL',
                      })}
                    </strong>
                  </article>
                  <article>
                    <span>Tempo médio de lavagem</span>
                    <strong>
                      {report.averageWashingMinutes === null
                        ? 'Sem dados'
                        : `${report.averageWashingMinutes} min`}
                    </strong>
                  </article>
                </div>
                <div className="report-tables">
                  <div>
                    <h3>Por serviço</h3>
                    {report.services.length === 0 ? (
                      <p className="empty-state">
                        Sem atendimentos no período.
                      </p>
                    ) : (
                      <table>
                        <thead>
                          <tr>
                            <th>Serviço</th>
                            <th>Qtd.</th>
                            <th>Faturamento</th>
                            <th>Comissões</th>
                          </tr>
                        </thead>
                        <tbody>
                          {report.services.map((service) => (
                            <tr key={service.serviceName}>
                              <td>{service.serviceName}</td>
                              <td>{service.orders}</td>
                              <td>
                                {service.revenue.toLocaleString('pt-BR', {
                                  style: 'currency',
                                  currency: 'BRL',
                                })}
                              </td>
                              <td>
                                {service.commissions.toLocaleString('pt-BR', {
                                  style: 'currency',
                                  currency: 'BRL',
                                })}
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    )}
                  </div>
                  <div>
                    <h3>Por forma de pagamento</h3>
                    {report.paymentMethods.length === 0 ? (
                      <p className="empty-state">Sem pagamentos no período.</p>
                    ) : (
                      <table>
                        <thead>
                          <tr>
                            <th>Forma</th>
                            <th>Qtd.</th>
                            <th>Valor</th>
                          </tr>
                        </thead>
                        <tbody>
                          {report.paymentMethods.map((payment) => (
                            <tr key={payment.method}>
                              <td>
                                {PAYMENT_LABELS[payment.method] ??
                                  payment.method}
                              </td>
                              <td>{payment.orders}</td>
                              <td>
                                {payment.amount.toLocaleString('pt-BR', {
                                  style: 'currency',
                                  currency: 'BRL',
                                })}
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    )}
                  </div>
                  <div>
                    <h3>Pausas registradas</h3>
                    {report.pauses.length === 0 ? (
                      <p className="empty-state">Nenhuma pausa no período.</p>
                    ) : (
                      <ul className="report-pause-list">
                        {report.pauses.map((pause) => (
                          <li key={pause.reason}>
                            <span>{PAUSE_LABELS[pause.reason]}</span>
                            <strong>{pause.count}</strong>
                          </li>
                        ))}
                      </ul>
                    )}
                  </div>
                </div>
              </>
            )}
            {!report && !reportLoading && !reportError && (
              <p className="empty-state">
                Selecione o período e gere o relatório.
              </p>
            )}
          </section>
        )}
        {role === 'manager' && (
          <section className="dashboard-section">
            <CashManager features={features} />
          </section>
        )}
      </div>
      {checklistModal && (
        <ChecklistModal
          features={features}
          serviceOrderId={checklistModal.serviceOrderId}
          type={checklistModal.type}
          onClose={() => setChecklistModal(null)}
        />
      )}
    </main>
  )
}
