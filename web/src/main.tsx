import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import '@/presentation/styles/global.css'
import { makeHomePage } from '@/main/factories/pages/make-home-page'

createRoot(document.getElementById('root')!).render(
  <StrictMode>{makeHomePage()}</StrictMode>,
)
