import type { ReactNode } from 'react'
import { Icon } from '../ui/Icon'
import styles from './PageHeader.module.css'

interface PageHeaderProps {
  title: string
  breadcrumb?: string
  right?: ReactNode
  /** Present only on pages reached by drilling in from somewhere else (task detail, so
   *  far) — renders a "← Back" control that returns to wherever the viewer actually came
   *  from (browser history), not a fixed route. A list page reached straight from the
   *  sidebar has nothing to go "back" to, so it omits this entirely. */
  onBack?: () => void
  /** A small pill rendered right next to the title itself — e.g. TaskDetailPage's NewBadge
   *  on a freshly-created task — for something that belongs at the very top of the page,
   *  not buried in a secondary row of badges further down. */
  titleBadge?: ReactNode
}

/** Breadcrumb + page title + right-side slot (filters, actions). Sits atop every page. */
export function PageHeader({ title, breadcrumb, right, onBack, titleBadge }: PageHeaderProps) {
  return (
    <div className={styles.header}>
      <div className={styles.leftGroup}>
        {onBack && (
          <button type="button" className={styles.backButton} onClick={onBack} aria-label="Go back">
            <Icon name="chevronLeft" size={18} />
          </button>
        )}
        <div>
          {breadcrumb && <div className={styles.breadcrumb}>{breadcrumb}</div>}
          <div className={styles.titleRow}>
            <h1 className={styles.title}>{title}</h1>
            {titleBadge}
          </div>
        </div>
      </div>
      {right && <div className={styles.right}>{right}</div>}
    </div>
  )
}
