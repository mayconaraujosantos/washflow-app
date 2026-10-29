import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import type {
  CashTransaction,
  CashTransactionReport,
  ExpenseCategory,
} from '@/domain/models/feature-models'
import type { Features } from '@/domain/usecases/features'

type Props = Readonly<{
  features: Features
}>

export function CashManager({ features }: Props) {
  const [transactions, setTransactions] = useState<CashTransaction[]>([])
  const [categories, setCategories] = useState<ExpenseCategory[]>([])
  const [report, setReport] = useState<CashTransactionReport | null>(null)
  const [loading, setLoading] = useState(false)

  // Form state
  const [type, setType] = useState<'INCOME' | 'EXPENSE'>('EXPENSE')
  const [amount, setAmount] = useState('')
  const [categoryId, setCategoryId] = useState('')
  const [description, setDescription] = useState('')
  const [referenceDate, setReferenceDate] = useState(() => {
    const d = new Date()
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  })

  // Report filters
  const [fromDate, setFromDate] = useState(() => {
    const d = new Date()
    const first = new Date(d.getFullYear(), d.getMonth(), 1)
    return `${first.getFullYear()}-${String(first.getMonth() + 1).padStart(2, '0')}-${String(first.getDate()).padStart(2, '0')}`
  })
  const [toDate, setToDate] = useState(() => {
    const d = new Date()
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  })

  const loadTransactions = useCallback(async () => {
    setLoading(true)
    try {
      const result = await features.listTransactions(fromDate || null, toDate || null)
      setTransactions(result)
    } catch (error) {
      console.error('Failed to load transactions:', error)
    } finally {
      setLoading(false)
    }
  }, [fromDate, toDate])

  const loadCategories = useCallback(async () => {
    try {
      const result = await features.listCategories()
      setCategories(result)
    } catch (error) {
      console.error('Failed to load categories:', error)
    }
  }, [])

  const loadReport = useCallback(async () => {
    if (!fromDate || !toDate) return
    try {
      const result = await features.transactionReport(fromDate, toDate)
      setReport(result)
    } catch (error) {
      console.error('Failed to load report:', error)
    }
  }, [fromDate, toDate])

  useEffect(() => {
    loadTransactions()
    loadCategories()
  }, [loadTransactions, loadCategories])

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    if (!amount) return
    try {
      await features.createTransaction(
        type,
        amount,
        referenceDate,
        categoryId || null,
        description || null,
        null,
      )
      setAmount('')
      setDescription('')
      setCategoryId('')
      loadTransactions()
    } catch (error) {
      console.error('Failed to create transaction:', error)
    }
  }

  const handleDelete = async (id: string) => {
    try {
      await features.deleteTransaction(id)
      setTransactions(prev => prev.filter(t => t.id !== id))
    } catch (error) {
      console.error('Failed to delete transaction:', error)
    }
  }

  const formatCurrency = (value: string) => {
    try {
      return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(
        parseFloat(value),
      )
    } catch {
      return value
    }
  }

  return (
    <div className="cash-manager">
      <h3>💰 Controle de Caixa</h3>

      {/* Report Summary */}
      <div className="cash-report-filters">
        <label>
          De:
          <input type="date" value={fromDate} onChange={e => setFromDate(e.target.value)} />
        </label>
        <label>
          Até:
          <input type="date" value={toDate} onChange={e => setToDate(e.target.value)} />
        </label>
        <button className="btn-secondary btn-small" onClick={loadReport}>
          Gerar relatório
        </button>
        <button className="btn-secondary btn-small" onClick={loadTransactions}>
          Atualizar
        </button>
      </div>

      {report && (
        <div className="cash-report-summary">
          <div className="report-card income">
            <span>Entradas</span>
            <strong>{formatCurrency(report.totalIncome)}</strong>
          </div>
          <div className="report-card expense">
            <span>Saídas</span>
            <strong>{formatCurrency(report.totalExpense)}</strong>
          </div>
          <div className="report-card balance">
            <span>Saldo</span>
            <strong>{formatCurrency(report.netBalance)}</strong>
          </div>
        </div>
      )}

      {/* New Transaction Form */}
      <form onSubmit={handleSubmit} className="cash-form">
        <div className="cash-form-fields">
          <label>
            Tipo:
            <select value={type} onChange={e => setType(e.target.value as 'INCOME' | 'EXPENSE')}>
              <option value="EXPENSE">Despesa</option>
              <option value="INCOME">Entrada</option>
            </select>
          </label>
          <label>
            Valor:
            <input
              type="number"
              step="0.01"
              min="0"
              placeholder="0.00"
              value={amount}
              onChange={e => setAmount(e.target.value)}
              required
            />
          </label>
          <label>
            Data:
            <input type="date" value={referenceDate} onChange={e => setReferenceDate(e.target.value)} />
          </label>
          <label>
            Categoria:
            <select value={categoryId} onChange={e => setCategoryId(e.target.value)}>
              <option value="">Sem categoria</option>
              {categories.map(cat => (
                <option key={cat.id} value={cat.id}>
                  {cat.name}
                </option>
              ))}
            </select>
          </label>
          <input
            type="text"
            placeholder="Descrição..."
            value={description}
            onChange={e => setDescription(e.target.value)}
          />
          <button type="submit" className="btn-primary">
            Adicionar
          </button>
        </div>
      </form>

      {/* Transaction List */}
      <div className="cash-list">
        {loading ? (
          <p>Carregando...</p>
        ) : transactions.length === 0 ? (
          <p>Nenhuma transação encontrada</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Data</th>
                <th>Tipo</th>
                <th>Valor</th>
                <th>Categoria</th>
                <th>Descrição</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {transactions.map(t => (
                <tr key={t.id} className={t.type === 'INCOME' ? 'income-row' : 'expense-row'}>
                  <td>{t.referenceDate}</td>
                  <td>
                    <span className={`badge ${t.type === 'INCOME' ? 'badge-green' : 'badge-red'}`}>
                      {t.type === 'INCOME' ? 'Entrada' : 'Despesa'}
                    </span>
                  </td>
                  <td className={t.type === 'INCOME' ? 'text-green' : 'text-red'}>
                    {t.type === 'INCOME' ? '+' : '-'}
                    {formatCurrency(t.amount)}
                  </td>
                  <td>
                    {t.categoryId && categories.find(c => c.id === t.categoryId)?.name || '—'}
                  </td>
                  <td>{t.description || '—'}</td>
                  <td>
                    <button className="btn-delete btn-small" onClick={() => handleDelete(t.id)}>
                      ✕
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}
