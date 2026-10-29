import { useCallback, useState } from 'react'
import type { ChangeEvent, FormEvent } from 'react'
import type { ServiceOrderPhoto } from '@/domain/models/feature-models'
import type { Features } from '@/domain/usecases/features'

type Props = Readonly<{
  features: Features
  serviceOrderId: string
}>

export function PhotoGallery({ features, serviceOrderId }: Props) {
  const [photos, setPhotos] = useState<ServiceOrderPhoto[]>([])
  const [loading, setLoading] = useState(false)
  const [imageData, setImageData] = useState('')
  const [description, setDescription] = useState('')
  const [photoType, setPhotoType] = useState<'ENTRY' | 'EXIT'>('ENTRY')
  const [uploading, setUploading] = useState(false)

  const loadPhotos = useCallback(async () => {
    setLoading(true)
    try {
      const result = await features.getPhotos(serviceOrderId)
      setPhotos(result)
    } catch (error) {
      console.error('Failed to load photos:', error)
    } finally {
      setLoading(false)
    }
  }, [serviceOrderId])

  const handleFileChange = (e: ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    const reader = new FileReader()
    reader.onload = () => {
      setImageData(reader.result as string)
    }
    reader.readAsDataURL(file)
  }

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    if (!imageData || uploading) return
    setUploading(true)
    try {
      const photo = await features.savePhoto(serviceOrderId, photoType, imageData, description || null)
      setPhotos(prev => [...prev, photo])
      setImageData('')
      setDescription('')
    } catch (error) {
      console.error('Failed to upload photo:', error)
    } finally {
      setUploading(false)
    }
  }

  const handleDelete = async (photoId: string) => {
    try {
      await features.deletePhoto(photoId)
      setPhotos(prev => prev.filter(p => p.id !== photoId))
    } catch (error) {
      console.error('Failed to delete photo:', error)
    }
  }

  return (
    <div className="photo-gallery">
      <h3>Fotos do veículo</h3>
      <button className="btn-secondary btn-small" onClick={loadPhotos} disabled={loading}>
        {loading ? 'Carregando...' : 'Carregar fotos'}
      </button>

      <form onSubmit={handleSubmit} className="photo-upload-form">
        <div className="photo-upload-fields">
          <label>
            Tipo:
            <select value={photoType} onChange={e => setPhotoType(e.target.value as 'ENTRY' | 'EXIT')}>
              <option value="ENTRY">Entrada</option>
              <option value="EXIT">Saída</option>
            </select>
          </label>
          <input type="file" accept="image/*" onChange={handleFileChange} />
          <input
            type="text"
            placeholder="Descrição..."
            value={description}
            onChange={e => setDescription(e.target.value)}
          />
          {imageData && (
            <img src={imageData} alt="Preview" className="photo-preview" />
          )}
          <button type="submit" className="btn-primary btn-small" disabled={!imageData || uploading}>
            {uploading ? 'Enviando...' : 'Enviar foto'}
          </button>
        </div>
      </form>

      {photos.length > 0 && (
        <div className="photo-grid">
          {photos.map(photo => (
            <div key={photo.id} className="photo-card">
              <img src={photo.imageData} alt={photo.description || 'Foto'} />
              <div className="photo-info">
                <span className="photo-type">
                  {photo.type === 'ENTRY' ? 'Entrada' : 'Saída'}
                </span>
                {photo.description && <span className="photo-desc">{photo.description}</span>}
                <button
                  className="btn-delete btn-small"
                  onClick={() => handleDelete(photo.id)}
                >
                  ✕
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
