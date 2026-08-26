import { NavLink } from 'react-router-dom'
import { Icon, type IconName } from '../ui/Icon'
import styles from './SidebarNavItem.module.css'

interface SidebarNavItemProps {
  to: string
  label: string
  icon: IconName
  count?: number
  end?: boolean
}

/** One nav row: icon, label, optional count, active state driven by the current route. */
export function SidebarNavItem({ to, label, icon, count, end }: SidebarNavItemProps) {
  return (
    <NavLink to={to} end={end} className={({ isActive }) => (isActive ? styles.itemActive : styles.item)}>
      <Icon name={icon} />
      <span className={styles.label}>{label}</span>
      {count !== undefined && <span className={styles.count}>{count}</span>}
    </NavLink>
  )
}
