import type { InputHTMLAttributes, ReactNode } from 'react'
import { Icon, type IconName } from '../../../components/ui/Icon'
import styles from './AuthField.module.css'

interface AuthFieldProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'onChange'> {
  label: string
  value: string
  onChange: (value: string) => void
  icon: IconName
  /** e.g. the show/hide password button — kept generic rather than a hardcoded "eye"
   *  slot since a leading icon + trailing control is the only shape this needs. */
  trailing?: ReactNode
}

/**
 * A labeled input with a leading icon (and optional trailing slot), purpose-built for the
 * login/signup forms — kept separate from the shared TextField rather than adding this
 * directly there, since TextField is used by more than a dozen forms across the app and
 * this shape (icon, absolute-positioned overlay) isn't something all of them need.
 */
export function AuthField({ label, value, onChange, icon, trailing, id, className, ...rest }: AuthFieldProps) {
  return (
    <label className={styles.field} htmlFor={id}>
      <span className={styles.label}>{label}</span>
      <div className={styles.inputWrap}>
        <span className={styles.leadingIcon}>
          <Icon name={icon} size={16} />
        </span>
        <input
          id={id}
          className={[styles.input, trailing && styles.inputWithTrailing].filter(Boolean).join(' ')}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          {...rest}
        />
        {trailing && <span className={styles.trailing}>{trailing}</span>}
      </div>
    </label>
  )
}
