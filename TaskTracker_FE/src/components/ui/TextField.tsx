import type { InputHTMLAttributes } from 'react'
import styles from './TextField.module.css'

interface TextFieldProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'onChange'> {
  label?: string
  value: string
  onChange: (value: string) => void
  error?: string
}

export function TextField({ label, value, onChange, error, id, ...rest }: TextFieldProps) {
  return (
    <label className={styles.field} htmlFor={id}>
      {label && <span className={styles.label}>{label}</span>}
      <input
        id={id}
        className={[styles.input, error && styles.inputError].filter(Boolean).join(' ')}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        {...rest}
      />
      {error && <span className={styles.error}>{error}</span>}
    </label>
  )
}
