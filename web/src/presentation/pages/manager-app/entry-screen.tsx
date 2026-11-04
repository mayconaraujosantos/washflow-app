import { SubmitEvent, useCallback, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import type { VehicleLookupModel } from '@/domain/models/vehicle-lookup-model'
import { toManausInstant } from '@/main/config/business-time'
import { UiIcon } from '@/presentation/components/ui-icon'
import { ScreenLayout } from '@/presentation/components/ui/app-chrome'
import { PhoneField } from '@/presentation/components/ui/auth-fields'
import {
  BottomSheet,
  EmptyState,
  Notice,
} from '@/presentation/components/ui/blocks'
import { SlotPicker } from '@/presentation/components/ui/schedule-pickers'
import {
  errorMessage,
  formatDateTime,
  formatMoney,
  formatPhone,
  normalizePlate,
} from '@/presentation/helpers/format'
import { useManager } from './manager-context'
import { ProductPicker } from './sale-screen'
import { VehicleSearch } from './vehicle-search'

type Props = Readonly<{ mode: 'entry' | 'schedule' }>

type NewClient = {
  plate: string
  name: string
  phone: string
  model: string
  color: string
}

/**
 * "Registrar entrada" (car at the counter -> straight to the yard) and
 * "Agendar". Follows the reference layout: customer search, "Nenhum cliente
 * selecionado" + "Adicionar novo cliente", expected delivery, check-list,
 * "Adicionar produto ou serviço" and the totals. Scheduling adds the free
 * slots of the chosen day.
 */
export function EntryScreen({ mode }: Props) {
  const {
    operations,
    servicePrices,
    notify,
    refresh,
    settings,
    establishment,
  } = useManager()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const isSchedule = mode === 'schedule'

  const [selected, setSelected] = useState<VehicleLookupModel | null>(null)
  const [newClient, setNewClient] = useState<NewClient | null>(null)
  const [clientSheet, setClientSheet] = useState<string | null>(null)
  const [servicePriceId, setServicePriceId] = useState<number | null>(null)
  const [pickerOpen, setPickerOpen] = useState(false)
  const [slot, setSlot] = useState<string | null>(null)
  const [delivery, setDelivery] = useState('')
  const [deliveryOpen, setDeliveryOpen] = useState(false)
  const [withChecklist, setWithChecklist] = useState(false)
  const [notes, setNotes] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const loadDay = useCallback(
    (date: string) => establishment.availability(date),
    [establishment],
  )

  const plate = selected?.plate ?? newClient?.plate ?? ''
  const service = servicePrices.find((item) => item.id === servicePriceId)
  const hasClient = selected !== null || newClient !== null

  const handleSubmit = async (event: SubmitEvent) => {
    event.preventDefault()
    setError(null)
    if (!hasClient) {
      setError('Busque o cliente ou adicione um novo cliente.')
      return
    }
    if (!service) {
      setError('Adicione o produto ou serviço.')
      return
    }
    if (isSchedule && !slot) {
      setError('Escolha um horário livre.')
      return
    }
    setSaving(true)
    try {
      const order = await operations.registerEntry({
        plate,
        servicePriceId: service.id,
        vehicleModel: newClient?.model || undefined,
        vehicleColor: newClient?.color || undefined,
        customerName: newClient?.name || undefined,
        customerPhone: newClient?.phone || undefined,
        scheduledAt: isSchedule && slot ? slot : undefined,
        expectedDeliveryAt: delivery ? toManausInstant(delivery) : undefined,
        notes: notes || undefined,
      })
      notify(
        'success',
        isSchedule
          ? `Agendamento de ${plate} criado.`
          : `Entrada de ${plate} registrada. Já está na fila do pátio.`,
      )
      await refresh()
      if (isSchedule) {
        navigate('/gerente/agenda')
      } else {
        navigate('/gerente/patio', {
          state: { ticketOrderId: order.id, checklist: withChecklist },
        })
      }
    } catch (cause) {
      setError(errorMessage(cause, 'Não foi possível registrar'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <ScreenLayout
      title={isSchedule ? 'Agendar lavagem' : 'Registrar entrada'}
      backTo="/gerente"
    >
      <form className="form os-form" onSubmit={handleSubmit}>
        {!hasClient && (
          <>
            <VehicleSearch
              selected={null}
              onSelect={(vehicle) => setSelected(vehicle)}
              onCreate={(typed) => setClientSheet(typed)}
            />
            <div className="client-box">
              <UiIcon name="alert" size={44} />
              <p>Nenhum cliente selecionado</p>
              <button
                type="button"
                className="btn btn-brand btn-pill-lg"
                onClick={() => setClientSheet('')}
              >
                Adicionar novo cliente
              </button>
            </div>
          </>
        )}

        {selected && (
          <VehicleSearch selected={selected} onSelect={setSelected} />
        )}
        {newClient && (
          <div className="client-chip">
            <div>
              <span className="plate">{newClient.plate}</span>
              <p className="muted">
                {[newClient.model, newClient.color]
                  .filter(Boolean)
                  .join(' · ') || 'Veículo novo'}
              </p>
              {(newClient.name || newClient.phone) && (
                <p className="faint">
                  {[newClient.name, formatPhone(newClient.phone)]
                    .filter(Boolean)
                    .join(' · ')}
                </p>
              )}
            </div>
            <button
              type="button"
              className="icon-btn"
              aria-label="Trocar cliente"
              onClick={() => setNewClient(null)}
            >
              <UiIcon name="close" />
            </button>
          </div>
        )}

        {isSchedule && (
          <section className="form">
            <h2 className="os-heading">Dia e horário</h2>
            <SlotPicker
              loadDay={loadDay}
              openOnSunday={settings?.openOnSunday ?? false}
              initialDate={searchParams.get('data')}
              value={slot}
              onChange={setSlot}
            />
          </section>
        )}

        <button
          type="button"
          className="outlined-field"
          onClick={() => setDeliveryOpen(true)}
        >
          <span className="outlined-legend">Previsão de entrega</span>
          <UiIcon name="calendar" size={18} />
          <span className="outlined-value">
            {delivery ? formatDateTime(toManausInstant(delivery)) : '—'}
          </span>
          <UiIcon name="chevronDown" size={18} />
        </button>

        {!isSchedule && (
          <button
            type="button"
            className="link-action"
            aria-pressed={withChecklist}
            onClick={() => setWithChecklist((value) => !value)}
          >
            <UiIcon name={withChecklist ? 'check' : 'list'} size={18} />
            {withChecklist ? 'Check-list ao registrar' : 'Check-list'}
          </button>
        )}

        <button
          type="button"
          className="link-action"
          onClick={() => setPickerOpen(true)}
        >
          <UiIcon name="cart" size={18} />
          {service
            ? 'Trocar produto ou serviço'
            : 'Adicionar produto ou serviço'}
        </button>

        <h2 className="os-heading">Produtos e serviços:</h2>
        {service ? (
          <ul className="list">
            <li className="line-item">
              <span>{service.name}</span>
              <strong>{formatMoney(service.price)}</strong>
              <button
                type="button"
                className="icon-btn"
                aria-label={`Remover ${service.name}`}
                onClick={() => setServicePriceId(null)}
              >
                <UiIcon name="trash" size={16} />
              </button>
            </li>
          </ul>
        ) : (
          <EmptyState icon="xBox" title="Nenhum produto selecionado" />
        )}

        <div className="totals">
          <span>Subtotal {formatMoney(service?.price ?? 0)}</span>
          <strong>Total {formatMoney(service?.price ?? 0)}</strong>
          <span className="faint">
            O pagamento é registrado na saída do veículo.
          </span>
        </div>

        <label className="field">
          <span>Observações</span>
          <textarea
            value={notes}
            maxLength={500}
            onChange={(event) => setNotes(event.target.value)}
            placeholder="Ex.: risco na porta traseira, cliente aguarda na loja"
          />
        </label>

        <Notice tone="error">{error}</Notice>

        <button
          type="submit"
          className="btn btn-accent btn-block"
          disabled={saving}
        >
          <UiIcon name={isSchedule ? 'calendar' : 'entry'} size={18} />
          {saving
            ? 'Salvando...'
            : isSchedule
              ? 'Confirmar agendamento'
              : 'Registrar entrada'}
        </button>
      </form>

      <NewClientSheet
        initialPlate={clientSheet}
        onClose={() => setClientSheet(null)}
        onSave={(client) => {
          setNewClient(client)
          setSelected(null)
          setClientSheet(null)
        }}
      />
      <ProductPicker
        open={pickerOpen}
        showQuantity={false}
        onClose={() => setPickerOpen(false)}
        onPick={(id) => {
          setServicePriceId(id)
          setPickerOpen(false)
        }}
      />
      <DeliverySheet
        open={deliveryOpen}
        value={delivery}
        onClose={() => setDeliveryOpen(false)}
        onSave={(value) => {
          setDelivery(value)
          setDeliveryOpen(false)
        }}
      />
    </ScreenLayout>
  )
}

/** "Novo cliente": only the plate is required; the rest helps reach the owner. */
function NewClientSheet({
  initialPlate,
  onClose,
  onSave,
}: Readonly<{
  initialPlate: string | null
  onClose: () => void
  onSave: (client: NewClient) => void
}>) {
  if (initialPlate === null) return null
  return (
    <BottomSheet open title="Novo cliente" onClose={onClose}>
      <NewClientForm
        key={initialPlate}
        initialPlate={initialPlate}
        onSave={onSave}
      />
    </BottomSheet>
  )
}

function NewClientForm({
  initialPlate,
  onSave,
}: Readonly<{ initialPlate: string; onSave: (client: NewClient) => void }>) {
  const [plate, setPlate] = useState(normalizePlate(initialPlate))
  const [name, setName] = useState('')
  const [phone, setPhone] = useState('')
  const [model, setModel] = useState('')
  const [color, setColor] = useState('')
  const valid = /^[A-Z]{3}\d[A-Z0-9]\d{2}$/.test(plate)

  return (
    <div className="form">
      <p className="notice notice-warning">Só a placa é obrigatória.</p>
      <label className="field">
        <span>Nome</span>
        <input
          className="input"
          value={name}
          maxLength={100}
          onChange={(event) => setName(event.target.value)}
          autoComplete="off"
        />
      </label>
      <PhoneField
        label="Telefone (WhatsApp)"
        value={phone}
        onChange={setPhone}
        required={false}
      />
      <label className="field">
        <span>Placa</span>
        <input
          className="input"
          value={plate}
          onChange={(event) => setPlate(normalizePlate(event.target.value))}
          placeholder="ABC1D23"
          autoCapitalize="characters"
        />
      </label>
      <label className="field">
        <span>Descrição do carro</span>
        <input
          className="input"
          value={model}
          maxLength={50}
          onChange={(event) => setModel(event.target.value)}
          placeholder="Onix"
        />
      </label>
      <label className="field">
        <span>Cor</span>
        <input
          className="input"
          value={color}
          maxLength={30}
          onChange={(event) => setColor(event.target.value)}
          placeholder="Prata"
        />
      </label>
      <button
        type="button"
        className="btn btn-accent btn-block"
        disabled={!valid}
        onClick={() =>
          onSave({
            plate,
            name: name.trim(),
            phone,
            model: model.trim(),
            color: color.trim(),
          })
        }
      >
        Salvar cliente
      </button>
    </div>
  )
}

/** Expected delivery: date and time of the shop (Manaus), optional. */
function DeliverySheet({
  open,
  value,
  onClose,
  onSave,
}: Readonly<{
  open: boolean
  value: string
  onClose: () => void
  onSave: (value: string) => void
}>) {
  const [date, setDate] = useState(value.slice(0, 10))
  const [time, setTime] = useState(value.slice(11, 16))
  return (
    <BottomSheet open={open} title="Previsão de entrega" onClose={onClose}>
      <div className="field-row">
        <label className="field">
          <span>Data</span>
          <input
            className="input"
            type="date"
            value={date}
            onChange={(event) => setDate(event.target.value)}
          />
        </label>
        <label className="field">
          <span>Horário</span>
          <input
            className="input"
            type="time"
            value={time}
            onChange={(event) => setTime(event.target.value)}
          />
        </label>
      </div>
      <div className="btn-row">
        <button
          type="button"
          className="btn btn-outline"
          onClick={() => onSave('')}
        >
          Sem previsão
        </button>
        <button
          type="button"
          className="btn btn-accent"
          disabled={!date || !time}
          onClick={() => onSave(`${date}T${time}`)}
        >
          Confirmar
        </button>
      </div>
    </BottomSheet>
  )
}
