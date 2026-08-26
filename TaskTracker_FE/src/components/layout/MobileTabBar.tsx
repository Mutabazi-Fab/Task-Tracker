import { NavLink } from 'react-router-dom'
import { Icon } from '../ui/Icon'
import { NAV_ITEMS } from './Sidebar'
import styles from './MobileTabBar.module.css'

/** Bottom tab bar, swapped in for the Sidebar below 768px. */
export function MobileTabBar() {
  return (
    <nav className={styles.bar}>
      {NAV_ITEMS.map((item) => (
        <NavLink
          key={item.to}
          to={item.to}
          end={item.end ?? false}
          className={({ isActive }) => (isActive ? styles.tabActive : styles.tab)}
        >
          <Icon name={item.icon} size={18} />
          <span>{item.label}</span>
        </NavLink>
      ))}
    </nav>
  )
}
