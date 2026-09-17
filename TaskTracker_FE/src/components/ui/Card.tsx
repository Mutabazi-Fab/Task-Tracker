import type { ReactNode } from 'react'
import styles from './Card.module.css'

interface CardProps {
  children: ReactNode
  padding?: 'xs' | 'sm' | 'md' | 'lg'
  className?: string
}

const PADDING_CLASS = {
  xs: 'paddingXs',
  sm: 'paddingSm',
  md: 'paddingMd',
  lg: 'paddingLg',
} as const

/** Bordered panel container. The base surface for almost everything. */
export function Card({ children, padding = 'md', className }: CardProps) {
  return <div className={[styles.card, styles[PADDING_CLASS[padding]], className].filter(Boolean).join(' ')}>{children}</div>
}
