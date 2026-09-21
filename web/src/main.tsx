import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Route, Routes } from 'react-router-dom'
import '@/presentation/styles/global.css'
import { makeHomePage } from '@/main/factories/pages/make-home-page'
import { makeCustomerQrCodePage } from '@/main/factories/pages/make-customer-qr-code-page'
import { makeCustomerLoginPage } from '@/main/factories/pages/make-customer-login-page'
import { makeCustomerDashboardPage } from '@/main/factories/pages/make-customer-dashboard-page'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <Routes>
        <Route path="/" element={makeHomePage()} />
        <Route path="/qrcodes/cliente" element={makeCustomerQrCodePage()} />
        <Route path="/login/cliente" element={makeCustomerLoginPage()} />
        <Route path="/cliente" element={makeCustomerDashboardPage()} />
      </Routes>
    </BrowserRouter>
  </StrictMode>,
)
