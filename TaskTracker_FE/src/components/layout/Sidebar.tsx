// Imported from routePaths directly, NOT from '../../app/routes' — that file (routes.tsx)
// imports AppShell, which imports this file, which would make ROUTES a circular import
// crashing at runtime ("Cannot access 'ROUTES' before initialization"). routePaths.ts has
// no imports of its own, so there's no cycle here.
import { ROUTES } from '../../app/routePaths'
import { ThemeToggle } from '../../features/theme/ThemeToggle'
import { useAuth } from '../../features/auth/useAuth'
import { Avatar } from '../ui/Avatar'
import { Icon } from '../ui/Icon'
import type { IconName } from '../ui/Icon'
import { SidebarLogo } from './SidebarLogo'
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
  const { currentUser, logout } = useAuth()

  return (
    <aside className={styles.sidebar}>
      <SidebarLogo />
      <nav className={styles.nav}>
        {NAV_ITEMS.map((item) => (
          <SidebarNavItem key={item.to} to={item.to} label={item.label} icon={item.icon} end={item.end ?? false} />
        ))}
      </nav>
      <div className={styles.footer}>
        {currentUser && (
          <div className={styles.user}>
            <Avatar name={currentUser.fullName} size="sm" />
            <div className={styles.userInfo}>
              <span className={styles.userName}>{currentUser.fullName}</span>
              <span className={styles.userRole}>
                {currentUser.role === 'DIRECTOR' ? 'Director' : currentUser.jobTitle}
              </span>
            </div>
            <button type="button" className={styles.logoutButton} onClick={logout} title="Log out" aria-label="Log out">
              <Icon name="logout" size={16} />
            </button>
          </div>
        )}
        <div className={styles.themeRow}>
          <ThemeToggle />
        </div>
      </div>
    </aside>
  )
}
