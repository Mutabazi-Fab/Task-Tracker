import { getInitials } from '../../lib/getInitials'
import styles from './Avatar.module.css'

interface AvatarProps {
  name: string
  size?: 'sm' | 'md' | 'lg'
}

/** Initials circle. Uses the accent wash — never a status colour. */
export function Avatar({ name, size = 'md' }: AvatarProps) {
  const sizeClass = size === 'sm' ? styles.sm : size === 'lg' ? styles.lg : styles.md
  return (
    <div className={[styles.avatar, sizeClass].join(' ')} title={name}>
      {getInitials(name)}
    </div>
  )
}
