import { NavLink } from 'react-router-dom'
import { useAuth } from '../../features/auth/useAuth'
import { Icon } from '../ui/Icon'
import { NAV_ITEMS } from './Sidebar'
import styles from './MobileTabBar.module.css'

/** Bottom tab bar, swapped in for the Sidebar below 768px. A "Log out" tab is appended
 *  here rather than left mobile-only-inaccessible, since the Sidebar's logout button
 *  doesn't render at all on this breakpoint. */
export function MobileTabBar() {
  const { logout } = useAuth()

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
      <button type="button" className={styles.tab} onClick={logout}>
        <Icon name="logout" size={18} />
        <span>Log out</span>
      </button>
    </nav>
  )
}
