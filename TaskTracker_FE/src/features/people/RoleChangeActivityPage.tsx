import { Navigate } from 'react-router-dom'
import { PageHeader } from '../../components/layout/PageHeader'
import { Card } from '../../components/ui/Card'
import { EmptyState } from '../../components/ui/EmptyState'
import { QueryBoundary } from '../../components/feedback/QueryBoundary'
import { ROUTES } from '../../app/routes'
import { formatDateTime } from '../../lib/formatDate'
import { useAuth } from '../auth/useAuth'
import { useRoleChangeActivity } from './hooks/useRoleChangeActivity'
import styles from './RoleChangeActivityPage.module.css'

/** Super-Admin-only — every role change ever made, org-wide. Not linked from the nav for
 *  anyone else, and redirects away outright if landed on directly. */
export function RoleChangeActivityPage() {
  const { currentUser, isSuperAdmin } = useAuth()
  const query = useRoleChangeActivity(currentUser?.id ?? NaN)

  if (!isSuperAdmin) {
    return <Navigate to={ROUTES.dashboard} replace />
  }

  return (
    <>
      <PageHeader breadcrumb="Throughline" title="Role Changes" />
      <Card>
        <QueryBoundary query={query}>
          {(activity) =>
            activity.length === 0 ? (
              <EmptyState title="No role changes yet" />
            ) : (
              <div>
                {activity.map((entry) => (
                  <div key={entry.id} className={styles.row}>
                    <div>
                      <div className={styles.personName}>{entry.personName}</div>
                      {entry.reason && <div className={styles.reason}>{entry.reason}</div>}
                    </div>
                    <span className={styles.roleChange}>
                      {entry.oldRole ?? 'none'} → {entry.newRole}
                    </span>
                    <span className={styles.changedBy}>by {entry.changedByName}</span>
                    <span className={styles.timestamp}>{formatDateTime(entry.timestamp)}</span>
                  </div>
                ))}
              </div>
            )
          }
        </QueryBoundary>
      </Card>
    </>
  )
}
