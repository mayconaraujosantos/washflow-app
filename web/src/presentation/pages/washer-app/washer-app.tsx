import { useCallback, useEffect, useState } from 'react'
import { Navigate, Route, Routes, useNavigate } from 'react-router-dom'
import type { ServiceOrderModel } from '@/domain/models/service-order-model'
import type { Operations, PauseReason } from '@/domain/usecases/operations'
import { PAUSE_LABELS } from '@/main/config/labels'
import {
  clearWasherSession,
  loadWasherSession,
} from '@/main/session/washer-session'
import { UiIcon } from '@/presentation/components/ui-icon'
import {
  ScreenLayout,
  type NavItem,
} from '@/presentation/components/ui/app-chrome'
import {
  BottomSheet,
  EmptyState,
  KpiBand,
  Notice,
  OrderCard,
} from '@/presentation/components/ui/blocks'
import {
  errorMessage,
  firstName,
  greeting,
} from '@/presentation/helpers/format'

type Props = Readonly<{ operations: Operations }>

const NAV: NavItem[] = [
  { to: '/lavador', label: 'Fila', icon: 'parking', end: true },
  { to: '/lavador/minhas', label: 'Minhas lavagens', icon: 'washer' },
]

const PAUSE_REASONS: PauseReason[] = ['LUNCH', 'RESTROOM', 'OTHER']
const REFRESH_MS = 3000

export function WasherApp({ operations }: Props) {
  const navigate = useNavigate()
  const [session] = useState(() => loadWasherSession())
  const user = session?.user ?? null

  const [queue, setQueue] = useState<ServiceOrderModel[]>([])
  const [mine, setMine] = useState<ServiceOrderModel[]>([])
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [busyId, setBusyId] = useState<string | null>(null)
  const [pausing, setPausing] = useState<ServiceOrderModel | null>(null)

  const refresh = useCallback(async () => {
    try {
      const [waiting, active] = await Promise.all([
        operations.loadQueue(),
        operations.loadMyOrders(),
      ])
      setQueue(waiting)
      setMine(active)
      setError(null)
    } catch (cause) {
      setError(errorMessage(cause, 'Falha ao atualizar o painel'))
    }
  }, [operations])

  useEffect(() => {
    if (!user) {
      navigate('/login/lavador', { replace: true })
      return
    }
    const first = window.setTimeout(() => void refresh(), 0)
    const timer = window.setInterval(() => void refresh(), REFRESH_MS)
    return () => {
      window.clearTimeout(first)
      window.clearInterval(timer)
    }
  }, [user, navigate, refresh])

  if (!user) return null

  const run = async (
    orderId: string,
    action: () => Promise<void>,
    message: string,
  ) => {
    setBusyId(orderId)
    setError(null)
    setNotice(null)
    try {
      await action()
      setNotice(message)
      await refresh()
    } catch (cause) {
      setError(errorMessage(cause, 'Falha ao atualizar atendimento'))
    } finally {
      setBusyId(null)
    }
  }

  const logout = () => {
    clearWasherSession()
    navigate('/login/lavador')
  }

  const header = (
    <>
      <div className="greeting">
        <div>
          <small>{greeting()},</small>
          <strong>{firstName(user.name)}</strong>
        </div>
      </div>
      <KpiBand
        items={[
          { label: 'Na fila', value: queue.length },
          { label: 'Minhas lavagens', value: mine.length },
          {
            label: 'Pausadas',
            value: mine.filter((order) => order.status === 'PAUSED').length,
          },
        ]}
      />
      <Notice tone="error">{error}</Notice>
      <Notice tone="success">{notice}</Notice>
    </>
  )

  const logoutAction = (
    <button
      type="button"
      className="icon-btn"
      aria-label="Sair"
      onClick={logout}
    >
      <UiIcon name="logout" size={22} />
    </button>
  )

  const queueScreen = (
    <ScreenLayout nav={NAV} actions={logoutAction}>
      {header}
      <h2>Fila do pátio</h2>
      {queue.length === 0 ? (
        <EmptyState
          icon="parking"
          title="Nenhum veículo aguardando."
          accent="A fila segue a ordem de chegada no pátio."
        />
      ) : (
        <ul className="list">
          {queue.map((order, index) => (
            <OrderCard key={order.id} order={order} showCustomer={false}>
              <button
                type="button"
                className={index === 0 ? 'btn btn-accent' : 'btn btn-outline'}
                disabled={busyId === order.id}
                onClick={() =>
                  void run(
                    order.id,
                    () => operations.start(order.id, user.id),
                    `Lavagem de ${order.details?.vehiclePlate ?? 'veículo'} iniciada.`,
                  )
                }
              >
                <UiIcon name="play" size={18} />
                Iniciar lavagem
              </button>
            </OrderCard>
          ))}
        </ul>
      )}
    </ScreenLayout>
  )

  const mineScreen = (
    <ScreenLayout nav={NAV} actions={logoutAction}>
      {header}
      <h2>Minhas lavagens</h2>
      {mine.length === 0 ? (
        <EmptyState
          icon="washer"
          title="Nenhuma lavagem em andamento."
          accent="Pegue o próximo carro na aba Fila."
        />
      ) : (
        <ul className="list">
          {mine.map((order) => (
            <OrderCard key={order.id} order={order} showCustomer={false}>
              {order.status === 'WASHING' ? (
                <>
                  <button
                    type="button"
                    className="btn btn-outline"
                    disabled={busyId === order.id}
                    onClick={() => setPausing(order)}
                  >
                    <UiIcon name="pause" size={18} />
                    Pausar
                  </button>
                  <button
                    type="button"
                    className="btn btn-accent"
                    disabled={busyId === order.id}
                    onClick={() =>
                      void run(
                        order.id,
                        () => operations.complete(order.id),
                        'Lavagem concluída. O gerente foi avisado pelo painel.',
                      )
                    }
                  >
                    <UiIcon name="check" size={18} />
                    Concluir lavagem
                  </button>
                </>
              ) : (
                <button
                  type="button"
                  className="btn btn-brand"
                  disabled={busyId === order.id}
                  onClick={() =>
                    void run(
                      order.id,
                      () => operations.resume(order.id),
                      'Lavagem retomada.',
                    )
                  }
                >
                  <UiIcon name="play" size={18} />
                  Retomar lavagem
                </button>
              )}
            </OrderCard>
          ))}
        </ul>
      )}
      <PauseSheet
        order={pausing}
        onClose={() => setPausing(null)}
        onConfirm={(order, reason, description) => {
          setPausing(null)
          void run(
            order.id,
            () => operations.pause(order.id, reason, description),
            `Lavagem pausada: ${PAUSE_LABELS[reason]}.`,
          )
        }}
      />
    </ScreenLayout>
  )

  return (
    <div className="app">
      <Routes>
        <Route index element={queueScreen} />
        <Route path="minhas" element={mineScreen} />
        <Route path="*" element={<Navigate to="/lavador" replace />} />
      </Routes>
    </div>
  )
}

function PauseSheet({
  order,
  onClose,
  onConfirm,
}: Readonly<{
  order: ServiceOrderModel | null
  onClose: () => void
  onConfirm: (
    order: ServiceOrderModel,
    reason: PauseReason,
    description?: string,
  ) => void
}>) {
  const [reason, setReason] = useState<PauseReason>('LUNCH')
  const [description, setDescription] = useState('')
  if (!order) return null
  const needsDescription = reason === 'OTHER' && !description.trim()

  return (
    <BottomSheet open title="Motivo da pausa" onClose={onClose}>
      <div role="group" aria-label="Motivo da pausa" className="form">
        {PAUSE_REASONS.map((option) => (
          <button
            key={option}
            type="button"
            className="sheet-option"
            aria-pressed={reason === option}
            onClick={() => setReason(option)}
          >
            <UiIcon name={reason === option ? 'check' : 'clock'} />
            {PAUSE_LABELS[option]}
          </button>
        ))}
      </div>
      {reason === 'OTHER' && (
        <label className="field">
          <span>Descreva o motivo</span>
          <input
            className="input"
            maxLength={200}
            value={description}
            onChange={(event) => setDescription(event.target.value)}
          />
        </label>
      )}
      <button
        type="button"
        className="btn btn-accent btn-block"
        disabled={needsDescription}
        onClick={() =>
          onConfirm(
            order,
            reason,
            reason === 'OTHER' ? description.trim() : undefined,
          )
        }
      >
        <UiIcon name="pause" size={18} />
        Pausar lavagem
      </button>
    </BottomSheet>
  )
}
