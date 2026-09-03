import { useMemo, useState } from 'react'
import { Navigate } from 'react-router-dom'
import { PageHeader } from '../../components/layout/PageHeader'
import { Card } from '../../components/ui/Card'
import { EmptyState } from '../../components/ui/EmptyState'
import { ErrorMessage } from '../../components/ui/ErrorMessage'
import { LoadingSpinner } from '../../components/ui/LoadingSpinner'
import { SegmentedControl } from '../../components/ui/SegmentedControl'
import { ROUTES } from '../../app/routes'
import { formatDateTime } from '../../lib/formatDate'
import { useAuth } from '../auth/useAuth'
import { useRoleChangeActivity } from './hooks/useRoleChangeActivity'
import { useAccountStatusChangeActivity } from './hooks/useAccountStatusChangeActivity'
import styles from './RoleChangeActivityPage.module.css'

type ActivityFilter = 'ALL' | 'ROLE' | 'STATUS'

const FILTER_OPTIONS: { label: string; value: ActivityFilter }[] = [
  { label: 'All', value: 'ALL' },
  { label: 'Role changes', value: 'ROLE' },
  { label: 'Account status', value: 'STATUS' },
]

/** One shared row shape both audit trails get normalized into, so "All" can merge and sort
 *  them by timestamp as a single feed instead of two separate lists. Adding a third kind of
 *  admin activity later (the door this was deliberately left open for) means adding one more
 *  mapper below, not a new page or a new filter mechanism. */
interface ActivityRow {
  id: string
  kind: Exclude<ActivityFilter, 'ALL'>
  personName: string
  reason: string | null
  changeLabel: string
  changedByName: string
  timestamp: string
}

/** Super-Admin-only — every role change AND account activation/deactivation ever made,
 *  org-wide, filterable by kind (or all of it, merged and sorted together). Not linked
 *  from the nav for anyone else, and redirects away outright if landed on directly. */
export function RoleChangeActivityPage() {
  const { currentUser, isSuperAdmin } = useAuth()
  const roleChangesQuery = useRoleChangeActivity(currentUser?.id ?? NaN)
  const statusChangesQuery = useAccountStatusChangeActivity(currentUser?.id ?? NaN)
  const [filter, setFilter] = useState<ActivityFilter>('ALL')

  const rows = useMemo<ActivityRow[] | undefined>(() => {
    if (!roleChangesQuery.data || !statusChangesQuery.data) return undefined

    const roleRows: ActivityRow[] = roleChangesQuery.data.map((entry) => ({
      id: `role-${entry.id}`,
      kind: 'ROLE',
      personName: entry.personName,
      reason: entry.reason,
      changeLabel: `${entry.oldRole ?? 'none'} → ${entry.newRole}`,
      changedByName: entry.changedByName,
      timestamp: entry.timestamp,
    }))

    const statusRows: ActivityRow[] = statusChangesQuery.data.map((entry) => ({
      id: `status-${entry.id}`,
      kind: 'STATUS',
      personName: entry.personName,
      reason: entry.reason,
      changeLabel: entry.active ? 'Reactivated' : 'Deactivated',
      changedByName: entry.changedByName,
      timestamp: entry.timestamp,
    }))

    return [...roleRows, ...statusRows].sort(
      (a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime(),
    )
  }, [roleChangesQuery.data, statusChangesQuery.data])

  if (!isSuperAdmin) {
    return <Navigate to={ROUTES.dashboard} replace />
  }

  const isLoading = roleChangesQuery.isLoading || statusChangesQuery.isLoading
  const error = roleChangesQuery.error ?? statusChangesQuery.error
  const visibleRows = rows?.filter((row) => filter === 'ALL' || row.kind === filter)

  return (
    <>
      <PageHeader
        breadcrumb="Throughline"
        title="Account Activity"
        right={<SegmentedControl options={FILTER_OPTIONS} value={filter} onChange={setFilter} aria-label="Filter activity" />}
      />
      <Card>
        {isLoading ? (
          <LoadingSpinner />
        ) : error ? (
          <ErrorMessage message={error.message} />
        ) : !visibleRows || visibleRows.length === 0 ? (
          <EmptyState title="No activity yet" />
        ) : (
          <div>
            {visibleRows.map((row) => (
              <div key={row.id} className={styles.row}>
                <div>
                  <div className={styles.personName}>{row.personName}</div>
                  {row.reason && <div className={styles.reason}>{row.reason}</div>}
                </div>
                <span className={styles.changeLabel}>{row.changeLabel}</span>
                <span className={styles.changedBy}>by {row.changedByName}</span>
                <span className={styles.timestamp}>{formatDateTime(row.timestamp)}</span>
              </div>
            ))}
          </div>
        )}
      </Card>
    </>
  )
}
