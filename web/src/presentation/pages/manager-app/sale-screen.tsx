import { SubmitEvent, useState } from 'react'
import type { SaleModel, SaleType } from '@/domain/models/sale-model'
import type { VehicleLookupModel } from '@/domain/models/vehicle-lookup-model'
import type { PaymentMethod } from '@/domain/usecases/operations'
import { PAYMENT_LABELS } from '@/main/config/labels'
import { UiIcon } from '@/presentation/components/ui-icon'
import { ScreenLayout } from '@/presentation/components/ui/app-chrome'
import {
  BottomSheet,
  EmptyState,
  Notice,
} from '@/presentation/components/ui/blocks'
import {
  errorMessage,
  formatMoney,
  parseMoney,
} from '@/presentation/helpers/format'
import { PAYMENT_METHODS, useManager } from './manager-context'
import { VehicleSearch } from './vehicle-search'

type Line = { servicePriceId: number; quantity: number }
type Payment = { method: PaymentMethod; amount: number }

const MAX_QUANTITY = 99

const roundCents = (value: number) => Math.round(value * 100) / 100

type Props = Readonly<{ type: SaleType }>

export function SaleScreen({ type }: Props) {
  const { operations, servicePrices, notify } = useManager()
  const isBudget = type === 'BUDGET'

  const [withClient, setWithClient] = useState(isBudget)
  const [selected, setSelected] = useState<VehicleLookupModel | null>(null)
  const [customerName, setCustomerName] = useState('')
  const [customerPhone, setCustomerPhone] = useState('')
  const [lines, setLines] = useState<Line[]>([])
  const [discount, setDiscount] = useState(0)
  const [payments, setPayments] = useState<Payment[]>([])
  const [fullMethod, setFullMethod] = useState<PaymentMethod>('PIX')
  const [partialOpen, setPartialOpen] = useState(false)
  const [partialMethod, setPartialMethod] = useState<PaymentMethod>('CASH')
  const [partialAmount, setPartialAmount] = useState('')
  const [notes, setNotes] = useState('')
  const [pickerOpen, setPickerOpen] = useState(false)
  const [discountOpen, setDiscountOpen] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [result, setResult] = useState<SaleModel | null>(null)

  const priceOf = (id: number) => servicePrices.find((price) => price.id === id)
  const subtotal = roundCents(
    lines.reduce(
      (sum, line) =>
        sum + (priceOf(line.servicePriceId)?.price ?? 0) * line.quantity,
      0,
    ),
  )
  const total = roundCents(Math.max(0, subtotal - discount))
  const paid = roundCents(payments.reduce((sum, item) => sum + item.amount, 0))
  const outstanding = roundCents(total - paid)

  const addLine = (servicePriceId: number, quantity: number) => {
    setLines((current) => {
      const existing = current.find(
        (line) => line.servicePriceId === servicePriceId,
      )
      if (!existing) return [...current, { servicePriceId, quantity }]
      return current.map((line) =>
        line === existing
          ? {
              ...line,
              quantity: Math.min(MAX_QUANTITY, line.quantity + quantity),
            }
          : line,
      )
    })
    setPayments([])
  }

  const changeQuantity = (servicePriceId: number, delta: number) => {
    setLines((current) =>
      current
        .map((line) =>
          line.servicePriceId === servicePriceId
            ? {
                ...line,
                quantity: Math.min(MAX_QUANTITY, line.quantity + delta),
              }
            : line,
        )
        .filter((line) => line.quantity > 0),
    )
    setPayments([])
  }

  const reset = () => {
    setSelected(null)
    setCustomerName('')
    setCustomerPhone('')
    setLines([])
    setDiscount(0)
    setPayments([])
    setNotes('')
    setResult(null)
    setError(null)
  }

  const handleSubmit = async (event: SubmitEvent) => {
    event.preventDefault()
    setError(null)
    if (lines.length === 0) {
      setError('Adicione pelo menos um produto ou serviço.')
      return
    }
    setSaving(true)
    try {
      const sale = await operations.createSale({
        type,
        items: lines,
        discount: discount || undefined,
        payments,
        vehicleId: withClient ? selected?.vehicleId : undefined,
        customerName:
          withClient && !selected
            ? customerName || undefined
            : (selected?.customerName ?? undefined),
        customerPhone:
          withClient && !selected
            ? customerPhone || undefined
            : (selected?.customerPhone ?? undefined),
        notes: notes || undefined,
      })
      setResult(sale)
      notify('success', isBudget ? 'Orçamento gerado.' : 'Venda registrada.')
    } catch (cause) {
      setError(errorMessage(cause, 'Não foi possível salvar'))
    } finally {
      setSaving(false)
    }
  }

  const title = isBudget ? 'Gerar orçamento' : 'Venda rápida'

  if (result) {
    return (
      <ScreenLayout title={title} backTo="/gerente">
        <section className="card">
          <EmptyState
            icon="check"
            title={isBudget ? 'Orçamento gerado!' : 'Venda registrada!'}
            description={`Total ${formatMoney(result.total)} · recebido ${formatMoney(result.paidAmount)}`}
            accent={
              result.outstandingAmount > 0
                ? `Saldo a receber: ${formatMoney(result.outstandingAmount)}`
                : undefined
            }
          />
          <ul className="list">
            {result.items.map((item) => (
              <li key={item.id} className="line-item">
                <span>{item.description}</span>
                <span className="muted">×{item.quantity}</span>
                <strong>{formatMoney(item.total)}</strong>
              </li>
            ))}
          </ul>
          <button
            type="button"
            className="btn btn-accent btn-block"
            onClick={reset}
          >
            {isBudget ? 'Novo orçamento' : 'Nova venda'}
          </button>
        </section>
      </ScreenLayout>
    )
  }

  return (
    <ScreenLayout title={title} backTo="/gerente">
      <form className="form" onSubmit={handleSubmit}>
        <label className="check">
          <input
            type="checkbox"
            checked={withClient}
            onChange={(event) => setWithClient(event.target.checked)}
          />
          Informar cliente?
        </label>

        {withClient && (
          <>
            <VehicleSearch selected={selected} onSelect={setSelected} />
            {!selected && (
              <div className="field-row">
                <label className="field">
                  <span>Nome (sem cadastro)</span>
                  <input
                    value={customerName}
                    maxLength={100}
                    onChange={(event) => setCustomerName(event.target.value)}
                  />
                </label>
                <label className="field">
                  <span>Telefone</span>
                  <input
                    type="tel"
                    inputMode="tel"
                    value={customerPhone}
                    onChange={(event) => setCustomerPhone(event.target.value)}
                  />
                </label>
              </div>
            )}
          </>
        )}

        <button
          type="button"
          className="btn btn-ghost"
          onClick={() => setPickerOpen(true)}
        >
          <UiIcon name="plus" size={18} />
          Adicionar produto ou serviço
        </button>

        <h2>Produtos e serviços</h2>
        {lines.length === 0 ? (
          <EmptyState icon="xBox" title="Nenhum produto selecionado" />
        ) : (
          <ul className="list line-items">
            {lines.map((line) => {
              const price = priceOf(line.servicePriceId)
              return (
                <li key={line.servicePriceId} className="line-item">
                  <span>
                    {price?.name}
                    <br />
                    <span className="faint">
                      {formatMoney(price?.price)} un.
                    </span>
                  </span>
                  <span className="qty">
                    <button
                      type="button"
                      className="icon-btn"
                      aria-label={`Diminuir ${price?.name}`}
                      onClick={() => changeQuantity(line.servicePriceId, -1)}
                    >
                      <UiIcon
                        name={line.quantity === 1 ? 'trash' : 'minus'}
                        size={16}
                      />
                    </button>
                    {line.quantity}
                    <button
                      type="button"
                      className="icon-btn"
                      aria-label={`Aumentar ${price?.name}`}
                      onClick={() => changeQuantity(line.servicePriceId, 1)}
                    >
                      <UiIcon name="plus" size={16} />
                    </button>
                  </span>
                  <strong>
                    {formatMoney((price?.price ?? 0) * line.quantity)}
                  </strong>
                </li>
              )
            })}
          </ul>
        )}

        <div className="totals">
          <span>Subtotal {formatMoney(subtotal)}</span>
          <span className="discount">
            Desconto {formatMoney(discount)}{' '}
            <button
              type="button"
              className="icon-btn"
              aria-label="Editar desconto"
              disabled={subtotal === 0}
              onClick={() => setDiscountOpen(true)}
            >
              <UiIcon name="edit" size={16} />
            </button>
          </span>
          <strong>Total {formatMoney(total)}</strong>
        </div>

        <h2>{isBudget ? 'Pagamento antecipado?' : 'Pagamento'}</h2>
        <p className="faint">
          Use o pagamento total ou abra as opções para dividir em valores e
          formas.
        </p>
        <div className="field-row">
          <label className="field">
            <span>Forma</span>
            <select
              value={fullMethod}
              onChange={(event) =>
                setFullMethod(event.target.value as PaymentMethod)
              }
            >
              {PAYMENT_METHODS.map((method) => (
                <option key={method} value={method}>
                  {PAYMENT_LABELS[method]}
                </option>
              ))}
            </select>
          </label>
          <button
            type="button"
            className="btn btn-outline"
            style={{ alignSelf: 'end' }}
            disabled={total === 0}
            onClick={() => setPayments([{ method: fullMethod, amount: total }])}
          >
            <UiIcon name="money" size={18} />
            Pagamento total
          </button>
        </div>

        <button
          type="button"
          className="btn btn-ghost"
          aria-expanded={partialOpen}
          onClick={() => setPartialOpen((open) => !open)}
        >
          Pagamento parcial ou mais de uma forma
          <UiIcon name="chevronDown" size={18} />
        </button>
        {partialOpen && (
          <div className="field-row">
            <label className="field">
              <span>Forma</span>
              <select
                value={partialMethod}
                onChange={(event) =>
                  setPartialMethod(event.target.value as PaymentMethod)
                }
              >
                {PAYMENT_METHODS.map((method) => (
                  <option key={method} value={method}>
                    {PAYMENT_LABELS[method]}
                  </option>
                ))}
              </select>
            </label>
            <label className="field">
              <span>Valor (R$)</span>
              <input
                inputMode="decimal"
                value={partialAmount}
                onChange={(event) => setPartialAmount(event.target.value)}
                placeholder="0,00"
              />
            </label>
            <button
              type="button"
              className="btn btn-outline"
              onClick={() => {
                const amount = parseMoney(partialAmount)
                if (!amount || amount > outstanding) {
                  setError(
                    'Valor parcial deve ser maior que zero e até o saldo.',
                  )
                  return
                }
                setError(null)
                setPayments((current) => [
                  ...current,
                  { method: partialMethod, amount },
                ])
                setPartialAmount('')
              }}
            >
              Adicionar pagamento
            </button>
          </div>
        )}

        {payments.length > 0 && (
          <ul className="list">
            {payments.map((payment, index) => (
              <li key={`${payment.method}-${index}`} className="line-item">
                <span>{PAYMENT_LABELS[payment.method]}</span>
                <strong>{formatMoney(payment.amount)}</strong>
                <button
                  type="button"
                  className="icon-btn"
                  aria-label="Remover pagamento"
                  onClick={() =>
                    setPayments((current) =>
                      current.filter((_, i) => i !== index),
                    )
                  }
                >
                  <UiIcon name="trash" size={16} />
                </button>
              </li>
            ))}
          </ul>
        )}
        <p className={`outstanding${outstanding > 0 ? ' is-due' : ''}`}>
          Saldo a receber: {formatMoney(outstanding)}
        </p>

        <label className="field">
          <span>Observações</span>
          <textarea
            value={notes}
            maxLength={500}
            onChange={(event) => setNotes(event.target.value)}
          />
        </label>

        <Notice tone="error">{error}</Notice>

        <button
          type="submit"
          className="btn btn-accent btn-block"
          disabled={saving}
        >
          {saving
            ? 'Salvando...'
            : isBudget
              ? 'Gerar orçamento'
              : 'Concluir venda'}
        </button>
      </form>

      <ProductPicker
        open={pickerOpen}
        onClose={() => setPickerOpen(false)}
        onPick={(id, quantity) => {
          addLine(id, quantity)
          setPickerOpen(false)
        }}
      />
      <DiscountSheet
        open={discountOpen}
        subtotal={subtotal}
        current={discount}
        onClose={() => setDiscountOpen(false)}
        onSave={(value) => {
          setDiscount(value)
          setPayments([])
          setDiscountOpen(false)
        }}
      />
    </ScreenLayout>
  )
}

/** Catalog picker in a bottom sheet; can also add a new product on the spot. */
export function ProductPicker({
  open,
  onClose,
  onPick,
  showQuantity = true,
}: Readonly<{
  open: boolean
  onClose: () => void
  onPick: (servicePriceId: number, quantity: number) => void
  // A service order has one service, so the entry form hides the quantity.
  showQuantity?: boolean
}>) {
  const { servicePrices, operations, reloadPrices } = useManager()
  const [query, setQuery] = useState('')
  const [quantity, setQuantity] = useState(1)
  const [creating, setCreating] = useState(false)
  const [name, setName] = useState('')
  const [price, setPrice] = useState('')
  const [commission, setCommission] = useState('')
  const [error, setError] = useState<string | null>(null)

  const filtered = servicePrices.filter((item) =>
    item.name.toLowerCase().includes(query.trim().toLowerCase()),
  )

  const createProduct = async () => {
    const parsedPrice = parseMoney(price)
    const parsedCommission = commission ? parseMoney(commission) : 0
    if (!name.trim() || !parsedPrice || parsedCommission === null) {
      setError('Informe nome e preço válidos (ex.: 25,90).')
      return
    }
    try {
      const created = await operations.createServicePrice({
        name: name.trim(),
        price: parsedPrice,
        washerCommission: parsedCommission,
      })
      await reloadPrices()
      setCreating(false)
      setName('')
      setPrice('')
      setCommission('')
      setError(null)
      onPick(created.id, quantity)
    } catch (cause) {
      setError(errorMessage(cause, 'Não foi possível criar o produto'))
    }
  }

  return (
    <BottomSheet
      open={open}
      title="Selecione o produto ou serviço"
      onClose={onClose}
    >
      {showQuantity && (
        <div className="quick-row">
          <button
            type="button"
            className="icon-btn"
            aria-label="Diminuir quantidade"
            onClick={() => setQuantity((value) => Math.max(1, value - 1))}
          >
            <UiIcon name="minus" />
          </button>
          <span className="field" style={{ alignItems: 'center' }}>
            <span>Quantidade</span>
            <strong>{quantity}</strong>
          </span>
          <button
            type="button"
            className="icon-btn"
            aria-label="Aumentar quantidade"
            onClick={() =>
              setQuantity((value) => Math.min(MAX_QUANTITY, value + 1))
            }
          >
            <UiIcon name="plus" />
          </button>
        </div>
      )}

      {creating ? (
        <div className="form">
          <label className="field">
            <span>Nome do produto ou serviço</span>
            <input
              className="input"
              value={name}
              maxLength={50}
              onChange={(event) => setName(event.target.value)}
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
          <div className="btn-row">
            <button
              type="button"
              className="btn btn-outline"
              onClick={() => setCreating(false)}
            >
              Cancelar
            </button>
            <button
              type="button"
              className="btn btn-accent"
              onClick={() => void createProduct()}
            >
              Criar e adicionar
            </button>
          </div>
        </div>
      ) : (
        <>
          <input
            className="input"
            type="search"
            aria-label="Buscar produto"
            placeholder="Buscar produto"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
          />
          {filtered.length === 0 ? (
            <EmptyState icon="cart" title="Nenhum produto para mostrar" />
          ) : (
            <div className="search-results">
              {filtered.map((item) => (
                <button
                  key={item.id}
                  type="button"
                  className="picker-item"
                  onClick={() => onPick(item.id, quantity)}
                >
                  <span>{item.name}</span>
                  <strong>{formatMoney(item.price)}</strong>
                </button>
              ))}
            </div>
          )}
          <button
            type="button"
            className="btn btn-ghost"
            onClick={() => setCreating(true)}
          >
            <UiIcon name="plus" size={18} />
            Criar novo produto
          </button>
        </>
      )}
    </BottomSheet>
  )
}

function DiscountSheet({
  open,
  subtotal,
  current,
  onClose,
  onSave,
}: Readonly<{
  open: boolean
  subtotal: number
  current: number
  onClose: () => void
  onSave: (value: number) => void
}>) {
  const [value, setValue] = useState(current ? String(current) : '')
  const [error, setError] = useState<string | null>(null)

  return (
    <BottomSheet open={open} title="Desconto" onClose={onClose}>
      <label className="field">
        <span>Valor do desconto (R$) · até {formatMoney(subtotal)}</span>
        <input
          className="input"
          inputMode="decimal"
          value={value}
          onChange={(event) => setValue(event.target.value)}
          placeholder="0,00"
        />
      </label>
      <Notice tone="error">{error}</Notice>
      <button
        type="button"
        className="btn btn-accent btn-block"
        onClick={() => {
          const parsed = value.trim() === '' ? 0 : parseMoney(value)
          if (parsed === null || parsed > subtotal) {
            setError('Desconto deve ficar entre zero e o subtotal.')
            return
          }
          setError(null)
          onSave(parsed)
        }}
      >
        Aplicar desconto
      </button>
    </BottomSheet>
  )
}
