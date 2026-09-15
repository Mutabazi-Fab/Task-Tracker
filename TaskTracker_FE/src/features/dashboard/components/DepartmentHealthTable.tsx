import { Link } from 'react-router-dom'
import { ROUTES } from '../../../app/routePaths'
import { QueryBoundary } from '../../../components/feedback/QueryBoundary'
import { EmptyState } from '../../../components/ui/EmptyState'
import { formatPercentage } from '../../../lib/formatPercentage'
import { useExecutiveDepartmentHealth } from '../hooks/useExecutiveDepartmentHealth'
import { DepartmentHealthBadge } from './DepartmentHealthBadge'
import styles from './DepartmentHealthTable.module.css'

/** Replaces the task-card list an Executive used to see — one row per department: name,
 *  head director, overall % complete, and derived health. Click a row to drill into
 *  DepartmentPage. Department count is small org-wide, so unlike TeamLeaderboardTable this
 *  isn't paginated. */
export function DepartmentHealthTable() {
  const query = useExecutiveDepartmentHealth()

  return (
    <QueryBoundary query={query}>
      {(departments) =>
        departments.length === 0 ? (
          <EmptyState title="No departments yet" />
        ) : (
          <div className={styles.scrollWrap}>
            <div className={styles.table}>
              <div className={styles.header}>
                <span>Department</span>
                <span>Head director</span>
                <span className={styles.headerRight}>Progress</span>
                <span className={styles.headerRight}>Health</span>
              </div>
              {departments.map((dept) => (
                <Link key={dept.id} to={ROUTES.department(dept.id)} className={styles.row}>
                  <span className={styles.name}>{dept.name}</span>
                  <span className={styles.director}>{dept.headDirectorName ?? '—'}</span>
                  <span className={styles.progress}>{formatPercentage(dept.averageProgress)}</span>
                  <span className={styles.healthCell}>
                    <DepartmentHealthBadge health={dept.health} />
                  </span>
                </Link>
              ))}
            </div>
          </div>
        )
      }
    </QueryBoundary>
  )
}
