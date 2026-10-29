import { useCallback, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import type { ChecklistItem, ChecklistType } from '@/domain/models/feature-models'
import { saveChecklist } from '@/domain/usecases/features'

const DEFAULT_ENTRY_ITEMS: ChecklistItem[] = [
  { label: 'Quilometragem', checked: false, note: null },
  { label: 'Nível do combustível', checked: false, note: null },
  { label: 'Rochas no para-choque', checked: false, note: null },
  { label: 'Arranhões visíveis', checked: false, note: null },
  { label: 'Amassados', checked: false, note: null },
  { label: 'Vidros quebrados', checked: false, note: null },
  { label: 'Aros riscados', checked: false, note: null },
  { label: 'Estofado sujo', checked: false, note: null },
  { label: 'Cheiro forte interno', checked: false, note: null },
  { label: 'Acessórios internos', checked: false, note: null },
]

const DEFAULT_EXIT_ITEMS: ChecklistItem[] = [
  { label: 'Carro limpo por fora', checked: false, note: null },
  { label: 'Carro limpo por dentro', checked: false, note: null },
  { label: 'Pneus limpos', checked: false, note: null },
  { label: 'Vidros limpos', checked: false, note: null },
  { label: 'Espelhos limpos', checked: false, note: null },
  { label: 'Placas limpas', checked: false, note: null },
  { label: 'Cheiro neutro/agradável', checked: false, note: null },
]

type Props = Readonly<{
  serviceOrderId: string
  type: ChecklistType
  onClose: () => void
}>

export function ChecklistModal({ serviceOrderId, type, onClose }: Props) {
  const isEntry = type === 'ENTRY'
  const [items, setItems] = useState<ChecklistItem[]>(
    isEntry ? DEFAULT_ENTRY_ITEMS : DEFAULT_EXIT_ITEMS,
  )
  const [saving, setSaving] = useState(false)
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const drawingRef = useRef(false)
  const lastPosRef = useRef({ x: 0, y: 0 })

  const initCanvas = useCallback(() => {
    const canvas = canvasRef.current
    if (!canvas) return
    const rect = canvas.getBoundingClientRect()
    canvas.width = rect.width * 2
    canvas.height = rect.height * 2
    const ctx = canvas.getContext('2d')
    if (!ctx) return
    ctx.scale(2, 2)
    ctx.strokeStyle = '#000'
    ctx.lineWidth = 2
    ctx.lineCap = 'round'
  }, [])

  const getCanvasPos = (e: React.MouseEvent | React.TouchEvent) => {
    const canvas = canvasRef.current
    if (!canvas) return { x: 0, y: 0 }
    const rect = canvas.getBoundingClientRect()
    if ('touches' in e) {
      return { x: e.touches[0].clientX - rect.left, y: e.touches[0].clientY - rect.top }
    }
    return { x: e.clientX - rect.left, y: e.clientY - rect.top }
  }

  const startDrawing = (e: React.MouseEvent | React.TouchEvent) => {
    drawingRef.current = true
    lastPosRef.current = getCanvasPos(e)
  }

  const draw = (e: React.MouseEvent | React.TouchEvent) => {
    if (!drawingRef.current) return
    const canvas = canvasRef.current
    const ctx = canvas?.getContext('2d')
    if (!canvas || !ctx) return
    const pos = getCanvasPos(e)
    ctx.beginPath()
    ctx.moveTo(lastPosRef.current.x, lastPosRef.current.y)
    ctx.lineTo(pos.x, pos.y)
    ctx.stroke()
    lastPosRef.current = pos
  }

  const stopDrawing = () => {
    drawingRef.current = false
  }

  const clearCanvas = () => {
    const canvas = canvasRef.current
    const ctx = canvas?.getContext('2d')
    if (!canvas || !ctx) return
    ctx.clearRect(0, 0, canvas.width, canvas.height)
  }

  const toggleItem = (index: number) => {
    setItems(prev =>
      prev.map((item, i) => (i === index ? { ...item, checked: !item.checked } : item)),
    )
  }

  const updateNote = (index: number, note: string) => {
    setItems(prev =>
      prev.map((item, i) => (i === index ? { ...item, note: note || null } : item)),
    )
  }

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    if (saving) return

    const signatureImage = canvasRef.current?.toDataURL('image/png')
    setSaving(true)
    try {
      await saveChecklist(serviceOrderId, type, items, signatureImage || null)
      onClose()
    } catch (error) {
      console.error('Failed to save checklist:', error)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="checklist-modal-overlay" onClick={onClose}>
      <div className="checklist-modal" onClick={e => e.stopPropagation()}>
        <h2>
          {isEntry ? 'Checklist de Entrada' : 'Checklist de Saída'}
        </h2>
        <form onSubmit={handleSubmit}>
          <div className="checklist-items">
            {items.map((item, index) => (
              <div key={index} className="checklist-item">
                <label>
                  <input
                    type="checkbox"
                    checked={item.checked}
                    onChange={() => toggleItem(index)}
                  />
                  <span>{item.label}</span>
                </label>
                {item.checked && (
                  <input
                    type="text"
                    placeholder="Observação..."
                    value={item.note || ''}
                    onChange={e => updateNote(index, e.target.value)}
                    className="checklist-note"
                  />
                )}
              </div>
            ))}
          </div>

          <div className="checklist-signature">
            <label>Assinatura:</label>
            <canvas
              ref={canvasRef}
              className="signature-canvas"
              onMouseDown={startDrawing}
              onMouseMove={draw}
              onMouseUp={stopDrawing}
              onMouseLeave={stopDrawing}
              onTouchStart={startDrawing}
              onTouchMove={draw}
              onTouchEnd={stopDrawing}
              onLoad={initCanvas}
            />
            <button type="button" className="btn-clear" onClick={clearCanvas}>
              Limpar assinatura
            </button>
          </div>

          <div className="checklist-actions">
            <button type="button" className="btn-secondary" onClick={onClose}>
              Cancelar
            </button>
            <button type="submit" className="btn-primary" disabled={saving}>
              {saving ? 'Salvando...' : 'Salvar checklist'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
