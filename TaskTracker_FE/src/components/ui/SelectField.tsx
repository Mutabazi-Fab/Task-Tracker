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
}

export function SelectField({ label, value, onChange, options, placeholder, id, disabled }: SelectFieldProps) {
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
          <option value="" disabled>
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
