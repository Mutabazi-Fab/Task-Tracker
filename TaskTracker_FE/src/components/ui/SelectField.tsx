import styles from './SelectField.module.css'

export interface SelectFieldOption {
  label: string
  value: string
}

interface SelectFieldProps {
  label?: string
  value: string
  onChange: (value: string) => void
  options: SelectFieldOption[]
  placeholder?: string
  id?: string
  disabled?: boolean
  /** When true, the placeholder is a real, always-selectable option (e.g. "All statuses" on
   *  a filter) instead of a disabled one — a disabled <option> can't be clicked back to once
   *  a real value has been picked, which is correct for a required field (CreateIncidentForm's
   *  "Select a business unit") but wrong for a filter that needs to clear back to "show
   *  everything". Defaults to false so every existing required-field usage is unaffected. */
  placeholderSelectable?: boolean
}

export function SelectField({ label, value, onChange, options, placeholder, id, disabled, placeholderSelectable }: SelectFieldProps) {
  return (
    <label className={styles.field} htmlFor={id}>
      {label && <span className={styles.label}>{label}</span>}
      <select
        id={id}
        className={styles.select}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        disabled={disabled}
      >
        {placeholder && (
          <option value="" disabled={!placeholderSelectable}>
            {placeholder}
          </option>
        )}
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    </label>
  )
}
