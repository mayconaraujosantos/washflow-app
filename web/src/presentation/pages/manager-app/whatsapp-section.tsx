import { useCallback, useEffect, useRef, useState } from 'react'
import type { WhatsappConnectionModel } from '@/domain/models/establishment-model'
import { Notice } from '@/presentation/components/ui/blocks'
import { errorMessage } from '@/presentation/helpers/format'
import { useManager } from './manager-context'

const STATUS_POLL_MS = 3_000
// WhatsApp rotates the QR code roughly every 40 seconds.
const QR_REFRESH_MS = 30_000

type View =
  | { kind: 'loading' }
  | { kind: 'unavailable'; message: string }
  | { kind: 'ready'; connection: WhatsappConnectionModel }

/**
 * Links the lava-jato's own WhatsApp number (QR code): customers then get "seu carro está pronto"
 * and appointment reminders, and can schedule by chatting with the number.
 */
export function WhatsappSection() {
  const { establishment, notify } = useManager()
  const [view, setView] = useState<View>({ kind: 'loading' })
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const waiting =
    view.kind === 'ready' && view.connection.state === 'WAITING_QR_CODE'
  const lastQr = useRef<string | null>(null)

  const show = useCallback((connection: WhatsappConnectionModel) => {
    if (connection.qrCode) lastQr.current = connection.qrCode
    setView({
      kind: 'ready',
      connection: {
        ...connection,
        qrCode:
          connection.state === 'WAITING_QR_CODE'
            ? (connection.qrCode ?? lastQr.current)
            : null,
      },
    })
  }, [])

  useEffect(() => {
    establishment
      .whatsappStatus()
      .then(show)
      .catch((cause: unknown) =>
        setView({
          kind: 'unavailable',
          message: errorMessage(cause, 'WhatsApp indisponível no momento.'),
        }),
      )
  }, [establishment, show])

  // While the QR code is on screen: watch for the scan and keep the code fresh.
  useEffect(() => {
    if (!waiting) return
    const poll = globalThis.setInterval(() => {
      establishment
        .whatsappStatus()
        .then((connection) => {
          if (connection.state === 'CONNECTED') {
            notify('success', 'WhatsApp conectado.')
          }
          show(connection)
        })
        .catch(() => undefined)
    }, STATUS_POLL_MS)
    const refresh = globalThis.setInterval(() => {
      establishment
        .connectWhatsapp()
        .then(show)
        .catch(() => undefined)
    }, QR_REFRESH_MS)
    return () => {
      globalThis.clearInterval(poll)
      globalThis.clearInterval(refresh)
    }
  }, [waiting, establishment, notify, show])

  const run = async (action: () => Promise<WhatsappConnectionModel>) => {
    setError(null)
    setBusy(true)
    try {
      show(await action())
    } catch (cause) {
      setError(errorMessage(cause, 'Não foi possível falar com o WhatsApp'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="card form">
      <h2>WhatsApp do lava-jato</h2>
      <p className="faint">
        Conecte o número do lava-jato: o cliente recebe o aviso de carro pronto
        e o lembrete do agendamento, e pode agendar conversando com o número.
      </p>
      {view.kind === 'loading' && <p className="muted">Carregando...</p>}
      {view.kind === 'unavailable' && <p className="muted">{view.message}</p>}
      {view.kind === 'ready' && view.connection.state === 'CONNECTED' && (
        <>
          <Notice tone="success">
            Conectado. O assistente está atendendo.
          </Notice>
          <button
            type="button"
            className="btn btn-outline"
            disabled={busy}
            onClick={() => run(() => establishment.disconnectWhatsapp())}
          >
            {busy ? 'Desconectando...' : 'Desconectar'}
          </button>
        </>
      )}
      {view.kind === 'ready' && view.connection.state === 'DISCONNECTED' && (
        <button
          type="button"
          className="btn btn-accent"
          disabled={busy}
          onClick={() => run(() => establishment.connectWhatsapp())}
        >
          {busy ? 'Gerando QR Code...' : 'Conectar WhatsApp'}
        </button>
      )}
      {waiting && (
        <>
          <ol className="faint">
            <li>Abra o WhatsApp no celular do lava-jato.</li>
            <li>Toque em Aparelhos conectados › Conectar um aparelho.</li>
            <li>Aponte a câmera para o código abaixo.</li>
          </ol>
          {view.connection.qrCode ? (
            <img
              src={view.connection.qrCode}
              alt="QR Code para conectar o WhatsApp"
              width={264}
              height={264}
            />
          ) : (
            <button
              type="button"
              className="btn btn-outline"
              disabled={busy}
              onClick={() => run(() => establishment.connectWhatsapp())}
            >
              Mostrar QR Code
            </button>
          )}
          <p className="faint">
            Use um número só do lava-jato: o assistente responde todas as
            conversas dele.
          </p>
        </>
      )}
      <Notice tone="error">{error}</Notice>
    </section>
  )
}
