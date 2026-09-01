import type { ReactNode } from 'react'
import { useMediaQuery } from '../../hooks/useMediaQuery'
import { SearchInput } from '../../features/search/components/SearchInput'
import { NotificationBell } from '../../features/notifications/components/NotificationBell'
import { Sidebar } from './Sidebar'
import { MobileTabBar } from './MobileTabBar'
import styles from './AppShell.module.css'

/**
 * Sidebar + main region wrapper. Below 768px the Sidebar is replaced by
 * MobileTabBar — handled here once, so no page has to duplicate the check.
 *
 * Also owns the persistent header bar above the page content, which exists
 * only to hold SearchInput. This is the one deliberate exception to "layout
 * components are feature-agnostic": a global search box must appear on
 * every route by construction, so its usage site can't live inside a
 * feature folder even though its implementation still does.
 */
export function AppShell({ children }: { children: ReactNode }) {
  const isMobile = useMediaQuery('(max-width: 768px)')

  return (
    <div className={styles.shell}>
      {!isMobile && <Sidebar />}
      <div className={styles.content}>
        <header className={styles.topBar}>
          <NotificationBell />
          <SearchInput />
        </header>
        <main className={isMobile ? styles.mainMobile : styles.main}>{children}</main>
      </div>
      {isMobile && <MobileTabBar />}
    </div>
  )
}
