import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import type { WasherStatusModel } from '@/domain/models/washer-status-model'
import { Notice } from '@/presentation/components/ui/blocks'
import {
  reservedText,
  washerStateText,
} from '@/presentation/helpers/washer-status'
import { useManager } from './manager-context'

const REFRESH_MS = 15_000
const STATE_ICON: Record<WasherStatusModel['state'], string> = {
  FREE: '🟢',
  WASHING: '🔵',
  PAUSED: '🟡',
}

/** The manager's view of the team right now: who is free, washing or on a pause. */
export function WasherBoard() {
  const { operations, yard } = useManager()
  const [washers, setWashers] = useState<WasherStatusModel[] | null>(null)
  const [, setTick] = useState(0)

  useEffect(() => {
    let active = true
    const load = () =>
      operations
        .loadWasherBoard()
        .then((board) => {
          if (active) setWashers(board)
        })
        .catch(() => undefined)
    void load()
    // Reload with the yard, and every few seconds so "há X min" keeps moving.
    const timer = globalThis.setInterval(() => {
      void load()
      setTick((tick) => tick + 1)
    }, REFRESH_MS)
    return () => {
      active = false
      globalThis.clearInterval(timer)
    }
  }, [operations, yard])

  if (washers === null) return null
  const free = washers.filter((washer) => washer.state === 'FREE').length

  return (
    <section className="card form" aria-label="Lavadores">
      <h2>
        Lavadores{' '}
        <span className="faint">
          ({free} {free === 1 ? 'livre' : 'livres'} de {washers.length})
        </span>
      </h2>
      {washers.length === 0 ? (
        <Notice tone="warning">
          Nenhum lavador ativo: os carros ficam esperando no pátio.{' '}
          <Link to="/gerente/equipe">Cadastrar lavador</Link>
        </Notice>
      ) : (
        <ul className="list">
          {washers.map((washer) => (
            <li key={washer.id} className="line-item">
              <span>
                {STATE_ICON[washer.state]} <strong>{washer.name}</strong>
              </span>
              <span className="faint">
                {[washerStateText(washer), reservedText(washer)]
                  .filter(Boolean)
                  .join(' · ')}
              </span>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
