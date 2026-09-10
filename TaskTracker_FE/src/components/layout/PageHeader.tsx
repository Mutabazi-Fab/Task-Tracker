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
}

/** Breadcrumb + page title + right-side slot (filters, actions). Sits atop every page. */
export function PageHeader({ title, breadcrumb, right, onBack }: PageHeaderProps) {
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
          <h1 className={styles.title}>{title}</h1>
        </div>
      </div>
      {right && <div className={styles.right}>{right}</div>}
    </div>
  )
}
