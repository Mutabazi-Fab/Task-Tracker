import type { ReactNode } from 'react'
import { useMediaQuery } from '../../hooks/useMediaQuery'
import { SearchInput } from '../../features/search/components/SearchInput'
import { NotificationBell } from '../../features/notifications/components/NotificationBell'
import { Sidebar } from './Sidebar'
import { MobileTabBar } from './MobileTabBar'
import styles from './AppShell.module.css'

/** Sidebar + main region wrapper. */
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
