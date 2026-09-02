// Imported from routePaths directly, NOT from '../../app/routes' — that file (routes.tsx)
// imports AppShell, which imports this file, which would make ROUTES a circular import
// crashing at runtime ("Cannot access 'ROUTES' before initialization"). routePaths.ts has
// no imports of its own, so there's no cycle here.
import { ROUTES } from '../../app/routePaths'
import { ThemeToggle } from '../../features/theme/ThemeToggle'
import { useAuth } from '../../features/auth/useAuth'
import { Avatar } from '../ui/Avatar'
import { Icon } from '../ui/Icon'
import { RoleBadge, resolveBadgeRole } from '../ui/RoleBadge'
import type { IconName } from '../ui/Icon'
import { SidebarLogo } from './SidebarLogo'
import { SidebarNavItem } from './SidebarNavItem'
import styles from './Sidebar.module.css'

type NavItem = { to: string; label: string; icon: IconName; end?: boolean }

const BASE_NAV_ITEMS: NavItem[] = [
  { to: ROUTES.dashboard, label: 'Dashboard', icon: 'dashboard', end: true },
  { to: ROUTES.tasks, label: 'Tasks', icon: 'tasks' },
  { to: ROUTES.people, label: 'People', icon: 'people' },
  { to: ROUTES.teams, label: 'Teams', icon: 'teams' },
]

/** Shared with MobileTabBar so the two never drift out of sync. A function, not a plain
 *  constant, since the Account Activity item only appears for a Super Admin. Route path
 *  (roleChanges) kept as-is — renaming it would just churn URLs for no benefit — even
 *  though the page and label now also cover account activation/deactivation. */
export function getNavItems(isSuperAdmin: boolean): NavItem[] {
  return isSuperAdmin
    ? [...BASE_NAV_ITEMS, { to: ROUTES.roleChanges, label: 'Account Activity', icon: 'shield' }]
    : BASE_NAV_ITEMS
}

export function Sidebar() {
  const { currentUser, isSuperAdmin, logout } = useAuth()
  const navItems = getNavItems(isSuperAdmin)

  return (
    <aside className={styles.sidebar}>
      <SidebarLogo />
      <nav className={styles.nav}>
        {navItems.map((item) => (
          <SidebarNavItem key={item.to} to={item.to} label={item.label} icon={item.icon} end={item.end ?? false} />
        ))}
      </nav>
      <div className={styles.footer}>
        {currentUser && (
          <div className={styles.user}>
            <Avatar name={currentUser.fullName} size="sm" />
            <div className={styles.userInfo}>
              <span className={styles.userName} title={currentUser.fullName}>
                {currentUser.fullName}
              </span>
              {/* Job title, always — role/leadership now lives solely in the RoleBadge below,
                  which carries its own distinct colour per state instead of repeating the
                  same info in plain text here too. title= gives the full text back on hover,
                  since both this and the name above truncate when they don't fit. */}
              <span className={styles.userRole} title={currentUser.jobTitle}>
                {currentUser.jobTitle}
              </span>
              <div className={styles.badgeRow}>
                <RoleBadge
                  badgeRole={resolveBadgeRole(
                    currentUser.role,
                    currentUser.teams.some((t) => t.isLeader),
                  )}
                />
              </div>
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
