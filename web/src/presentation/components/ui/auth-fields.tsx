import { useId, useState } from 'react'
import { UiIcon } from '@/presentation/components/ui-icon'
import {
  maskPhoneInput,
  phoneDigitsFromInput,
} from '@/presentation/helpers/format'

function BrazilFlag() {
  return (
    <svg
      aria-hidden="true"
      width="28"
      height="20"
      viewBox="0 0 28 20"
      focusable="false"
    >
      <rect width="28" height="20" rx="2" fill="#009c3b" />
      <path d="M14 2.5 25.5 10 14 17.5 2.5 10Z" fill="#ffdf00" />
      <circle cx="14" cy="10" r="4.6" fill="#002776" />
      <path
        d="M9.6 9.2c3-.9 6.1-.5 8.8 1"
        stroke="#fff"
        strokeWidth="0.9"
        fill="none"
      />
    </svg>
  )
}

type PhoneFieldProps = Readonly<{
  label?: string
  value: string
  onChange: (digits: string) => void
  required?: boolean
}>

/** Brazilian phone: "+55" prefix, masked as (92) 99123-4567, value kept as digits. */
export function PhoneField({
  label = 'Telefone',
  value,
  onChange,
  required = true,
}: PhoneFieldProps) {
  const id = useId()
  return (
    <div className="field">
      <label htmlFor={id} className="visually-hidden">
        {label}
      </label>
      <div className="adorned-input">
        <span className="adornment" aria-hidden="true">
          <BrazilFlag />
          +55
        </span>
        <input
          id={id}
          type="tel"
          inputMode="tel"
          autoComplete="tel-national"
          placeholder={label}
          value={maskPhoneInput(value)}
          onChange={(event) =>
            onChange(phoneDigitsFromInput(event.target.value, value))
          }
          required={required}
        />
      </div>
    </div>
  )
}

type PasswordFieldProps = Readonly<{
  label?: string
  value: string
  onChange: (value: string) => void
  autoComplete?: 'current-password' | 'new-password'
}>

export function PasswordField({
  label = 'Senha',
  value,
  onChange,
  autoComplete = 'current-password',
}: PasswordFieldProps) {
  const id = useId()
  const [visible, setVisible] = useState(false)
  return (
    <div className="field">
      <label htmlFor={id} className="visually-hidden">
        {label}
      </label>
      <div className="adorned-input">
        <input
          id={id}
          type={visible ? 'text' : 'password'}
          autoComplete={autoComplete}
          placeholder={label}
          minLength={8}
          value={value}
          onChange={(event) => onChange(event.target.value)}
          required
        />
        <button
          type="button"
          className="icon-btn"
          aria-label={visible ? 'Ocultar senha' : 'Mostrar senha'}
          onClick={() => setVisible((current) => !current)}
        >
          <UiIcon name={visible ? 'eyeOff' : 'eye'} size={22} />
        </button>
      </div>
    </div>
  )
}

/** Plain text field whose label reads as the placeholder, like the phone/password fields. */
export function TextField({
  label,
  value,
  onChange,
  autoComplete,
  inputMode,
  maxLength = 100,
}: Readonly<{
  label: string
  value: string
  onChange: (value: string) => void
  autoComplete?: string
  inputMode?: 'text' | 'email'
  maxLength?: number
}>) {
  const id = useId()
  return (
    <div className="field">
      <label htmlFor={id} className="visually-hidden">
        {label}
      </label>
      <input
        id={id}
        className="input"
        placeholder={label}
        autoComplete={autoComplete}
        inputMode={inputMode}
        autoCapitalize={inputMode === 'email' ? 'none' : undefined}
        maxLength={maxLength}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        required
      />
    </div>
  )
}
