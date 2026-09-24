import { NavLink } from 'react-router-dom'
import { useAuth } from '../../features/auth/useAuth'
import { Icon } from '../ui/Icon'
import { getNavItems } from './Sidebar'
import styles from './MobileTabBar.module.css'

/** Bottom tab bar, swapped in for the Sidebar below 768px. */
export function MobileTabBar() {
  const { isDirector, logout } = useAuth()
  const navItems = getNavItems(isDirector)

  return (
    <nav className={styles.bar}>
      {navItems.map((item) => (
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
      <button type="button" className={styles.tab} onClick={logout}>
        <Icon name="logout" size={18} />
        <span>Log out</span>
      </button>
    </nav>
  )
}
