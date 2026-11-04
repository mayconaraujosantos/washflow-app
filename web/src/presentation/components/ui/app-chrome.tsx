import type { ReactNode } from 'react'
import { Link, NavLink } from 'react-router-dom'
import { UiIcon, type UiIconName } from '@/presentation/components/ui-icon'
import { useEscape } from './use-escape'

/** Washflow's own mark: a droplet over a car silhouette. */
export function BrandLogo({ size = 'sm' }: Readonly<{ size?: 'sm' | 'lg' }>) {
  const px = size === 'lg' ? 96 : 34
  return (
    <span className={`brand brand-${size}`}>
      <svg
        aria-hidden="true"
        width={px}
        height={px}
        viewBox="0 0 64 64"
        fill="none"
        focusable="false"
      >
        <path
          d="M32 4c6 9 12 15 12 22a12 12 0 0 1-24 0c0-7 6-13 12-22Z"
          fill="var(--brand)"
        />
        <path
          d="M30 18c-3 4-5 7-5 9"
          stroke="var(--brand-ink)"
          strokeWidth="2.5"
          strokeLinecap="round"
        />
        <path
          d="M10 46l4-9a6 6 0 0 1 5.5-3.5h25A6 6 0 0 1 50 37l4 9v8a2 2 0 0 1-2 2h-4a2 2 0 0 1-2-2v-2H18v2a2 2 0 0 1-2 2h-4a2 2 0 0 1-2-2v-8Z"
          fill="var(--accent)"
        />
        <circle cx="19" cy="47" r="3" fill="var(--accent-ink)" />
        <circle cx="45" cy="47" r="3" fill="var(--accent-ink)" />
        <path d="M17 41h30" stroke="var(--accent-ink)" strokeWidth="2" />
      </svg>
      <span className="brand-word">
        Wash<b>flow</b>
      </span>
    </span>
  )
}

type TopBarProps = Readonly<{
  title?: string
  onMenu?: () => void
  backTo?: string
  actions?: ReactNode
}>

/** Logo in the middle on tab screens; back arrow + title on inner screens. */
export function AppTopBar({ title, onMenu, backTo, actions }: TopBarProps) {
  let leading: ReactNode = <span />
  if (backTo) {
    leading = (
      <Link to={backTo} className="icon-btn" aria-label="Voltar">
        <UiIcon name="back" size={24} />
      </Link>
    )
  } else if (onMenu) {
    leading = (
      <button
        type="button"
        className="icon-btn"
        aria-label="Abrir menu"
        onClick={onMenu}
      >
        <UiIcon name="menu" size={24} />
      </button>
    )
  }

  return (
    <header className="topbar">
      {leading}
      {title ? (
        <h1 className="topbar-title">{title}</h1>
      ) : (
        <div className="topbar-center">
          <BrandLogo />
        </div>
      )}
      <div className="topbar-actions">{actions}</div>
    </header>
  )
}

export type NavItem = Readonly<{
  to: string
  label: string
  icon: UiIconName
  end?: boolean
}>

export function BottomNav({ items }: Readonly<{ items: NavItem[] }>) {
  return (
    <nav className="bottom-nav" aria-label="Navegação principal">
      {items.map((item) => (
        <NavLink key={item.to} to={item.to} end={item.end}>
          <UiIcon name={item.icon} size={22} />
          {item.label}
        </NavLink>
      ))}
    </nav>
  )
}

export type MenuItem = Readonly<{
  label: string
  icon: UiIconName
  to?: string
  onClick?: () => void
}>

type SideMenuProps = Readonly<{
  open: boolean
  onClose: () => void
  title: string
  subtitle?: string
  items: MenuItem[]
  footer?: MenuItem[]
}>

export function SideMenu({
  open,
  onClose,
  title,
  subtitle,
  items,
  footer = [],
}: SideMenuProps) {
  useEscape(open, onClose)
  if (!open) return null

  const renderItem = (item: MenuItem) =>
    item.to ? (
      <NavLink key={item.label} to={item.to} onClick={onClose}>
        <UiIcon name={item.icon} size={20} />
        {item.label}
      </NavLink>
    ) : (
      <button
        key={item.label}
        type="button"
        className="drawer-item"
        onClick={() => {
          onClose()
          item.onClick?.()
        }}
      >
        <UiIcon name={item.icon} size={20} />
        {item.label}
      </button>
    )

  return (
    <>
      <div className="drawer-backdrop" aria-hidden="true" onClick={onClose} />
      <nav className="drawer" aria-label="Menu">
        <div className="drawer-head">
          <strong>{title}</strong>
          {subtitle && <span className="faint">{subtitle}</span>}
        </div>
        {items.map(renderItem)}
        <div className="drawer-spacer" />
        {footer.map(renderItem)}
      </nav>
    </>
  )
}

type ScreenLayoutProps = Readonly<{
  title?: string
  backTo?: string
  onMenu?: () => void
  actions?: ReactNode
  nav?: NavItem[]
  children: ReactNode
}>

/** Top bar + scrollable content + (on tab screens) the bottom navigation. */
export function ScreenLayout({
  title,
  backTo,
  onMenu,
  actions,
  nav,
  children,
}: ScreenLayoutProps) {
  return (
    <>
      <AppTopBar
        title={title}
        backTo={backTo}
        onMenu={onMenu}
        actions={actions}
      />
      <main className={`app-content${nav ? '' : ' no-nav'}`}>{children}</main>
      {nav && <BottomNav items={nav} />}
    </>
  )
}

/** Centered brand + card used by the public login pages. */
export function AuthLayout({
  title,
  subtitle,
  children,
  footer,
}: Readonly<{
  title: string
  subtitle?: ReactNode
  children: ReactNode
  footer?: ReactNode
}>) {
  return (
    <main className="auth-shell">
      <BrandLogo size="lg" />
      <section className="auth-card">
        <h1>{title}</h1>
        {subtitle && <p className="subtitle">{subtitle}</p>}
        {children}
      </section>
      {footer}
    </main>
  )
}
