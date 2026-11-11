import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Route, Routes } from 'react-router-dom'
import '@/presentation/styles/global.css'
import { registerPwa } from '@/main/config/pwa'
import { adoptEstablishmentFromUrl } from '@/main/session/establishment-choice'
import {
  makeAdminLoginPage,
  makeAdminPanelPage,
} from '@/main/factories/pages/make-admin-pages'
import { errorReporter } from '@/main/config/error-reporting'
import { ErrorBoundary } from '@/presentation/components/error-boundary'
import { makeHomePage } from '@/main/factories/pages/make-home-page'
import { makeSetupPage } from '@/main/factories/pages/make-setup-page'
import {
  makeMagicLinkPage,
  makeTicketPage,
} from '@/main/factories/pages/make-access-pages'
import { makeCustomerQrCodePage } from '@/main/factories/pages/make-customer-qr-code-page'
import { makeCustomerLoginPage } from '@/main/factories/pages/make-customer-login-page'
import { makeCustomerDashboardPage } from '@/main/factories/pages/make-customer-dashboard-page'
import { makeWasherQrCodePage } from '@/main/factories/pages/make-washer-qr-code-page'
import { makeWasherLoginPage } from '@/main/factories/pages/make-washer-login-page'
import { makeManagerQrCodePage } from '@/main/factories/pages/make-manager-qr-code-page'
import { makeManagerLoginPage } from '@/main/factories/pages/make-manager-login-page'
import { makeWasherDashboardPage } from '@/main/factories/pages/make-washer-dashboard-page'
import { makeManagerDashboardPage } from '@/main/factories/pages/make-manager-dashboard-page'

registerPwa()
// Shop link / QR code: /?lavajato=<address> preselects the customer's lava-jato.
adoptEstablishmentFromUrl()
errorReporter.install()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ErrorBoundary
      onError={(error, source) => errorReporter.report(error, source)}
    >
      <BrowserRouter>
        <Routes>
          <Route path="/" element={makeHomePage()} />
          <Route path="/configurar" element={makeSetupPage()} />
          <Route path="/ticket/:token" element={makeTicketPage()} />
          <Route path="/entrar/whatsapp" element={makeMagicLinkPage()} />
          <Route path="/entrar/email" element={makeMagicLinkPage()} />
          <Route path="/qrcodes/cliente" element={makeCustomerQrCodePage()} />
          <Route path="/login/cliente" element={makeCustomerLoginPage()} />
          <Route path="/cliente/*" element={makeCustomerDashboardPage()} />
          <Route path="/qrcodes/lavador" element={makeWasherQrCodePage()} />
          <Route path="/login/lavador" element={makeWasherLoginPage()} />
          <Route path="/qrcodes/gerente" element={makeManagerQrCodePage()} />
          <Route path="/login/gerente" element={makeManagerLoginPage()} />
          <Route path="/lavador/*" element={makeWasherDashboardPage()} />
          <Route path="/gerente/*" element={makeManagerDashboardPage()} />
          <Route path="/login/admin" element={makeAdminLoginPage()} />
          <Route path="/admin" element={makeAdminPanelPage()} />
        </Routes>
      </BrowserRouter>
    </ErrorBoundary>
  </StrictMode>,
)
