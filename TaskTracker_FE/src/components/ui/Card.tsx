import type { ReactNode } from 'react'
import styles from './Card.module.css'

interface CardProps {
  children: ReactNode
  padding?: 'sm' | 'md' | 'lg'
  className?: string
}

/** Bordered panel container. The base surface for almost everything. */
export function Card({ children, padding = 'md', className }: CardProps) {
  const paddingClass =
    padding === 'sm' ? styles.paddingSm : padding === 'lg' ? styles.paddingLg : styles.paddingMd
  return <div className={[styles.card, paddingClass, className].filter(Boolean).join(' ')}>{children}</div>
}
