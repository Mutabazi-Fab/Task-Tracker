import type { ReactNode } from 'react'
import styles from './PageHeader.module.css'

interface PageHeaderProps {
  title: string
  breadcrumb?: string
  right?: ReactNode
}

/** Breadcrumb + page title + right-side slot (filters, actions). Sits atop every page. */
export function PageHeader({ title, breadcrumb, right }: PageHeaderProps) {
  return (
    <div className={styles.header}>
      <div>
        {breadcrumb && <div className={styles.breadcrumb}>{breadcrumb}</div>}
        <h1 className={styles.title}>{title}</h1>
      </div>
      {right && <div className={styles.right}>{right}</div>}
    </div>
  )
}
