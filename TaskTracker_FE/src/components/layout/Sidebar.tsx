import { ROUTES } from '../../app/routes'
import { ThemeToggle } from '../../features/theme/ThemeToggle'
import type { IconName } from '../ui/Icon'
import { SidebarNavItem } from './SidebarNavItem'
import styles from './Sidebar.module.css'

/** Shared with MobileTabBar so the two never drift out of sync. */
export const NAV_ITEMS: { to: string; label: string; icon: IconName; end?: boolean }[] = [
  { to: ROUTES.dashboard, label: 'Dashboard', icon: 'dashboard', end: true },
  { to: ROUTES.tasks, label: 'Tasks', icon: 'tasks' },
  { to: ROUTES.people, label: 'People', icon: 'people' },
  { to: ROUTES.teams, label: 'Teams', icon: 'teams' },
]

export function Sidebar() {
  return (
    <aside className={styles.sidebar}>
      <div className={styles.brand}>
        <span className={styles.brandMark}>THROUGHLINE</span>
      </div>
      <nav className={styles.nav}>
        {NAV_ITEMS.map((item) => (
          <SidebarNavItem key={item.to} to={item.to} label={item.label} icon={item.icon} end={item.end ?? false} />
        ))}
      </nav>
      <div className={styles.footer}>
        <ThemeToggle />
      </div>
    </aside>
  )
}
