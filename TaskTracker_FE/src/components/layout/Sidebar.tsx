// Imported from routePaths directly, not '../../app/routes' — routes.tsx imports AppShell,
// which imports this file, which would make ROUTES a circular import.
import { ROUTES } from '../../app/routePaths'
import { ThemeToggle } from '../../features/theme/ThemeToggle'
import { useAuth } from '../../features/auth/useAuth'
import { useUnreadCountsByType } from '../../features/notifications/hooks/useUnreadCountsByType'
import { usePendingExtensionRequests } from '../../features/taskDetail/hooks/usePendingExtensionRequests'
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

/** Shared with MobileTabBar. */
export function getNavItems(isDirector: boolean): NavItem[] {
  const items = [...BASE_NAV_ITEMS]
  if (isDirector) {
    items.push({ to: ROUTES.departments, label: 'Departments', icon: 'departments' })
    items.push({ to: ROUTES.incidents, label: 'Incidents', icon: 'alert' })
    items.push({ to: ROUTES.requests, label: 'Requests', icon: 'mail' })
    items.push({ to: ROUTES.activity, label: 'Activity', icon: 'shield' })
  }
  return items
}

/** Every count > 0 shown as a small badge on the nav item — "something new since you last looked."
 *  Tasks/Teams/Departments/Activity ride on the notification system, cleared when the corresponding
 *  page is opened (see each page's useMarkCategoryRead). */
function useNavBadgeCounts(hasUser: boolean, isDirector: boolean) {
  const typeCounts = useUnreadCountsByType(hasUser)
  const pendingRequests = usePendingExtensionRequests(isDirector)

  function countFor(routeTo: string): number | undefined {
    const raw =
      routeTo === ROUTES.tasks
        ? (typeCounts.data?.TASK_ASSIGNED ?? 0) +
          (typeCounts.data?.SUBTASK_ASSIGNED ?? 0) +
          (typeCounts.data?.TASK_REASSIGNED ?? 0) +
          (typeCounts.data?.SUBTASK_REASSIGNED ?? 0)
      : routeTo === ROUTES.teams ? typeCounts.data?.TEAM_CREATED
      : routeTo === ROUTES.departments ? typeCounts.data?.DEPARTMENT_CREATED
      : routeTo === ROUTES.activity ? typeCounts.data?.TASK_DELETED
      : routeTo === ROUTES.requests ? pendingRequests.data?.length
      : undefined
    return raw && raw > 0 ? raw : undefined
  }

  return countFor
}

export function Sidebar() {
  const { currentUser, isDirector, logout } = useAuth()
  const navItems = getNavItems(isDirector)
  const countFor = useNavBadgeCounts(!!currentUser, isDirector)

  return (
    <aside className={styles.sidebar}>
      <SidebarLogo />
      <nav className={styles.nav}>
        {navItems.map((item) => (
          <SidebarNavItem
            key={item.to}
            to={item.to}
            label={item.label}
            icon={item.icon}
            end={item.end ?? false}
            count={countFor(item.to)}
          />
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
              {/* Job title only — role/leadership lives in the RoleBadge below instead. */}
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
