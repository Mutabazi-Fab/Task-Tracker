import type { ButtonHTMLAttributes } from 'react'
import styles from './Button.module.css'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger'
}

const VARIANT_CLASS = {
  primary: styles.primary,
  secondary: styles.secondary,
  ghost: styles.ghost,
  danger: styles.danger,
} as const

export function Button({ variant = 'primary', className, ...rest }: ButtonProps) {
  return <button className={[styles.base, VARIANT_CLASS[variant], className].filter(Boolean).join(' ')} {...rest} />
}
