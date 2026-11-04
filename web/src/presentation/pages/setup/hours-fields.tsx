export type HoursValue = {
  openingHour: number
  closingHour: number
  openOnSunday: boolean
  maxVehiclesPerHour: number
}

type Props = Readonly<{
  value: HoursValue
  onChange: (value: HoursValue) => void
}>

const clamp = (value: number, min: number, max: number) =>
  Number.isNaN(value) ? min : Math.min(max, Math.max(min, value))

/** Opening hours (whole hours, Manaus time), Sunday flag and yard capacity per hour. */
export function HoursFields({ value, onChange }: Props) {
  const set = (patch: Partial<HoursValue>) => onChange({ ...value, ...patch })
  return (
    <>
      <div className="field-row">
        <label className="field">
          <span>Abre às (h)</span>
          <input
            className="input"
            type="number"
            inputMode="numeric"
            min={0}
            max={23}
            value={value.openingHour}
            onChange={(event) =>
              set({ openingHour: clamp(Number(event.target.value), 0, 23) })
            }
            required
          />
        </label>
        <label className="field">
          <span>Fecha às (h)</span>
          <input
            className="input"
            type="number"
            inputMode="numeric"
            min={1}
            max={24}
            value={value.closingHour}
            onChange={(event) =>
              set({ closingHour: clamp(Number(event.target.value), 1, 24) })
            }
            required
          />
        </label>
      </div>
      <label className="check">
        <input
          type="checkbox"
          checked={value.openOnSunday}
          onChange={(event) => set({ openOnSunday: event.target.checked })}
        />
        Abre aos domingos
      </label>
      <label className="field">
        <span>Capacidade do pátio (veículos por hora)</span>
        <input
          className="input"
          type="number"
          inputMode="numeric"
          min={1}
          max={50}
          value={value.maxVehiclesPerHour}
          onChange={(event) =>
            set({
              maxVehiclesPerHour: clamp(Number(event.target.value), 1, 50),
            })
          }
          required
        />
      </label>
    </>
  )
}
