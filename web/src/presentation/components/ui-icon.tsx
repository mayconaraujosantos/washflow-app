import type { ReactNode } from 'react'

export type UiIconName =
  | 'calendar'
  | 'car'
  | 'chart'
  | 'check'
  | 'clock'
  | 'clipboard'
  | 'download'
  | 'logout'
  | 'manager'
  | 'pause'
  | 'play'
  | 'refresh'
  | 'sparkles'
  | 'washer'

const ICONS: Record<UiIconName, ReactNode> = {
  calendar: (
    <>
      <rect x="3" y="5" width="18" height="16" rx="2" />
      <path d="M16 3v4M8 3v4M3 11h18" />
    </>
  ),
  car: (
    <>
      <path d="m5 11 1.5-5h11l1.5 5" />
      <path d="M3 11h18l1 3v5h-3v-2H5v2H2v-5l1-3Z" />
      <circle cx="6.5" cy="14" r=".75" />
      <circle cx="17.5" cy="14" r=".75" />
    </>
  ),
  chart: (
    <>
      <path d="M4 19V5M4 19h17" />
      <path d="m7 15 4-4 3 2 6-7" />
      <path d="M16 6h4v4" />
    </>
  ),
  check: <path d="m5 12 4 4L19 6" />,
  clock: (
    <>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 7v5l3 2" />
    </>
  ),
  clipboard: (
    <>
      <rect x="5" y="4" width="14" height="17" rx="2" />
      <path d="M9 4.5h6a1.5 1.5 0 0 0-3-1 1.5 1.5 0 0 0-3 1ZM9 11h6M9 15h6" />
    </>
  ),
  download: (
    <>
      <path d="M12 3v12m-5-5 5 5 5-5" />
      <path d="M5 17v4h14v-4" />
    </>
  ),
  logout: (
    <>
      <path d="M10 17l5-5-5-5M15 12H3" />
      <path d="M12 3h7a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-7" />
    </>
  ),
  manager: (
    <>
      <path d="M3 21h18M5 21V7l7-4 7 4v14M9 21v-6h6v6" />
      <path d="M9 9h.01M15 9h.01" />
    </>
  ),
  pause: <path d="M9 5v14M15 5v14" />,
  play: <path d="m8 5 12 7-12 7V5Z" />,
  refresh: (
    <>
      <path d="M20 7v5h-5M4 17v-5h5" />
      <path d="M5.5 9A7 7 0 0 1 18 6l2 6M4 12l2 6a7 7 0 0 0 12.5-3" />
    </>
  ),
  sparkles: (
    <>
      <path d="m12 3 1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8L12 3Z" />
      <path d="m19 14 .9 2.1L22 17l-2.1.9L19 20l-.9-2.1L16 17l2.1-.9L19 14ZM5 3l.7 1.3L7 5l-1.3.7L5 7l-.7-1.3L3 5l1.3-.7L5 3Z" />
    </>
  ),
  washer: (
    <>
      <path d="M5 3h14l2 3v15H3V6l2-3Z" />
      <circle cx="12" cy="13" r="5" />
      <path d="M7 7h.01M10 7h.01M13 7h.01" />
    </>
  ),
}

type Props = Readonly<{
  name: UiIconName
  size?: number
  className?: string
}>

export function UiIcon({ name, size = 18, className }: Props) {
  return (
    <svg
      aria-hidden="true"
      className={className}
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      focusable="false"
    >
      {ICONS[name]}
    </svg>
  )
}
