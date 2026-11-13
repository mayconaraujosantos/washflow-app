import { SubmitEvent, useState } from 'react'
import type { EstablishmentModel } from '@/domain/models/establishment-model'
import { UiIcon } from '@/presentation/components/ui-icon'
import { ScreenLayout } from '@/presentation/components/ui/app-chrome'
import { Notice } from '@/presentation/components/ui/blocks'
import {
  errorMessage,
  formatBusinessHours,
  formatMoney,
  parseMoney,
} from '@/presentation/helpers/format'
import {
  HoursFields,
  type HoursValue,
} from '@/presentation/pages/setup/hours-fields'
import { useManager } from './manager-context'
import { WhatsappSection } from './whatsapp-section'

/** Establishment parameters (hours, Sundays, yard capacity) and the service catalog. */
export function SettingsScreen() {
  const { settings } = useManager()
  return (
    <ScreenLayout title="Estabelecimento" backTo="/gerente">
      {settings ? (
        <EstablishmentForm
          key={settings.configuredAt ?? 'new'}
          current={settings}
        />
      ) : (
        <p className="muted">Carregando...</p>
      )}
      <WhatsappSection />
      <CatalogSection />
    </ScreenLayout>
  )
}

function EstablishmentForm({
  current,
}: Readonly<{ current: EstablishmentModel }>) {
  const { establishment, reloadSettings, notify } = useManager()
  const [name, setName] = useState(current.name)
  const [hours, setHours] = useState<HoursValue>({
    openingHour: current.openingHour,
    closingHour: current.closingHour,
    openOnSunday: current.openOnSunday,
    maxVehiclesPerHour: current.maxVehiclesPerHour,
  })
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  const save = async (event: SubmitEvent) => {
    event.preventDefault()
    setError(null)
    if (hours.openingHour >= hours.closingHour) {
      setError('A abertura deve ser antes do fechamento.')
      return
    }
    setSaving(true)
    try {
      await establishment.update({ name: name.trim(), ...hours })
      await reloadSettings()
      notify('success', 'Parâmetros do estabelecimento salvos.')
    } catch (cause) {
      setError(errorMessage(cause, 'Não foi possível salvar'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <form className="card form" onSubmit={save}>
      <h2>Funcionamento e pátio</h2>
      <p className="faint">
        Hoje: {formatBusinessHours(current)} · até {current.maxVehiclesPerHour}{' '}
        veículos por hora. Valem para agendamentos de clientes e do balcão.
      </p>
      <label className="field">
        <span>Nome do lava-jato</span>
        <input
          className="input"
          value={name}
          maxLength={100}
          onChange={(event) => setName(event.target.value)}
          required
        />
      </label>
      <HoursFields value={hours} onChange={setHours} />
      <Notice tone="error">{error}</Notice>
      <button type="submit" className="btn btn-accent" disabled={saving}>
        {saving ? 'Salvando...' : 'Salvar parâmetros'}
      </button>
    </form>
  )
}

function CatalogSection() {
  const { servicePrices, operations, reloadPrices, notify } = useManager()
  const [name, setName] = useState('')
  const [price, setPrice] = useState('')
  const [commission, setCommission] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  const add = async (event: SubmitEvent) => {
    event.preventDefault()
    setError(null)
    const parsedPrice = parseMoney(price)
    const parsedCommission = commission ? parseMoney(commission) : 0
    if (!parsedPrice || parsedCommission === null) {
      setError('Informe preço e comissão válidos (ex.: 45,00).')
      return
    }
    setSaving(true)
    try {
      await operations.createServicePrice({
        name: name.trim(),
        price: parsedPrice,
        washerCommission: parsedCommission,
      })
      await reloadPrices()
      notify('success', `${name.trim()} adicionado ao catálogo.`)
      setName('')
      setPrice('')
      setCommission('')
    } catch (cause) {
      setError(errorMessage(cause, 'Não foi possível adicionar'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <section className="card form">
      <h2>Serviços e produtos</h2>
      <ul className="list">
        {servicePrices.map((item) => (
          <li key={item.id} className="line-item">
            <span>{item.name}</span>
            <span className="faint">
              comissão {formatMoney(item.washerCommission)}
            </span>
            <strong>{formatMoney(item.price)}</strong>
          </li>
        ))}
      </ul>
      <form className="form" onSubmit={add}>
        <h3>Novo serviço ou produto</h3>
        <label className="field">
          <span>Nome</span>
          <input
            className="input"
            value={name}
            maxLength={50}
            onChange={(event) => setName(event.target.value)}
            required
          />
        </label>
        <div className="field-row">
          <label className="field">
            <span>Preço (R$)</span>
            <input
              className="input"
              inputMode="decimal"
              value={price}
              onChange={(event) => setPrice(event.target.value)}
              placeholder="0,00"
              required
            />
          </label>
          <label className="field">
            <span>Comissão do lavador (R$)</span>
            <input
              className="input"
              inputMode="decimal"
              value={commission}
              onChange={(event) => setCommission(event.target.value)}
              placeholder="0,00"
            />
          </label>
        </div>
        <Notice tone="error">{error}</Notice>
        <button type="submit" className="btn btn-outline" disabled={saving}>
          <UiIcon name="plus" size={18} />
          {saving ? 'Adicionando...' : 'Adicionar ao catálogo'}
        </button>
      </form>
    </section>
  )
}
