import type { InputHTMLAttributes, ReactNode } from 'react'
import styles from './TextField.module.css'

interface TextFieldProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'onChange'> {
  label?: string
  value: string
  onChange: (value: string) => void
  error?: string
  /** e.g. a show/hide password toggle — optional, so the other forms using TextField are
   *  unaffected; only rendered (and only then does the input gain right padding) when passed. */
  trailing?: ReactNode
}

export function TextField({ label, value, onChange, error, trailing, id, ...rest }: TextFieldProps) {
  return (
    <label className={styles.field} htmlFor={id}>
      {label && <span className={styles.label}>{label}</span>}
      <div className={styles.inputWrap}>
        <input
          id={id}
          className={[styles.input, error && styles.inputError, trailing && styles.inputWithTrailing]
            .filter(Boolean)
            .join(' ')}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          {...rest}
        />
        {trailing && <span className={styles.trailing}>{trailing}</span>}
      </div>
      {error && <span className={styles.error}>{error}</span>}
    </label>
  )
}
