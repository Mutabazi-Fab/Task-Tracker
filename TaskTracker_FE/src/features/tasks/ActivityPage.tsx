import { useEffect, useMemo, useState } from 'react'
import { Navigate } from 'react-router-dom'
import { PageHeader } from '../../components/layout/PageHeader'
import { Card } from '../../components/ui/Card'
import { EmptyState } from '../../components/ui/EmptyState'
import { ErrorMessage } from '../../components/ui/ErrorMessage'
import { LoadingSpinner } from '../../components/ui/LoadingSpinner'
import { Pagination } from '../../components/ui/Pagination'
import { SegmentedControl } from '../../components/ui/SegmentedControl'
import { ROUTES } from '../../app/routePaths'
import { formatDateTime } from '../../lib/formatDate'
import { useAuth } from '../auth/useAuth'
import { useRoleChangeActivity } from '../people/hooks/useRoleChangeActivity'
import { useAccountStatusChangeActivity } from '../people/hooks/useAccountStatusChangeActivity'
import { useDepartmentActivity } from '../departments/hooks/useDepartmentActivity'
import { useMarkCategoryRead } from '../notifications/hooks/useMarkCategoryRead'
import { useTaskActivity } from './hooks/useTaskActivity'
import styles from './ActivityPage.module.css'

type ActivityFilter = 'ALL' | 'TASK' | 'ROLE' | 'STATUS' | 'DEPARTMENT'

const FILTER_OPTIONS: { label: string; value: ActivityFilter }[] = [
  { label: 'All', value: 'ALL' },
  { label: 'Tasks', value: 'TASK' },
  { label: 'Roles', value: 'ROLE' },
  { label: 'Account status', value: 'STATUS' },
  { label: 'Departments', value: 'DEPARTMENT' },
]

const PAGE_SIZE = 15
// Every source is fetched as one bounded batch and merged client-side — an org's
// admin-action volume is bounded by headcount, so this holds up without a server-side
// merged query across four tables.
const FETCH_SIZE = 100

type Row =
  | { id: string; kind: 'TASK'; timestamp: string; taskCode: string; title: string; parentTaskCode: string | null; parentTaskTitle: string | null; action: 'CREATED' | 'DELETED'; assigneeSummary: string; actorName: string }
  | { id: string; kind: 'ROLE'; timestamp: string; personName: string; changeLabel: string; reason: string | null; actorName: string }
  | { id: string; kind: 'STATUS'; timestamp: string; personName: string; changeLabel: string; reason: string | null; actorName: string }
  | { id: string; kind: 'DEPARTMENT'; timestamp: string; departmentName: string; actorName: string }

/** Director or Super Admin only — every notable admin action in the org in one feed: task
 *  creation/deletion, role changes, account (de)activation, department deletion. */
export function ActivityPage() {
  const { currentUser, isDirector } = useAuth()
  const [filter, setFilter] = useState<ActivityFilter>('ALL')
  const [page, setPage] = useState(0)

  const taskQuery = useTaskActivity(0, FETCH_SIZE)
  const roleQuery = useRoleChangeActivity(currentUser?.id ?? NaN)
  const statusQuery = useAccountStatusChangeActivity(currentUser?.id ?? NaN)
  const departmentQuery = useDepartmentActivity(0, FETCH_SIZE)
  const markCategoryRead = useMarkCategoryRead()

  // Clears the Sidebar's "new activity" badge — runs unconditionally since this component
  // only ever renders for a Director-or-above (the redirect below handles anyone else).
  useEffect(() => {
    markCategoryRead.mutate(['TASK_DELETED'])
  }, [])

  const rows = useMemo<Row[] | undefined>(() => {
    if (!taskQuery.data || !roleQuery.data || !statusQuery.data || !departmentQuery.data) return undefined

    const taskRows: Row[] = taskQuery.data.content.map((entry) => ({
      id: `task-${entry.id}`,
      kind: 'TASK',
      timestamp: entry.timestamp,
      taskCode: entry.taskCode,
      title: entry.title,
      parentTaskCode: entry.parentTaskCode,
      parentTaskTitle: entry.parentTaskTitle,
      action: entry.action,
      assigneeSummary: entry.assigneeSummary,
      actorName: entry.performedByName,
    }))

    const roleRows: Row[] = roleQuery.data.map((entry) => ({
      id: `role-${entry.id}`,
      kind: 'ROLE',
      timestamp: entry.timestamp,
      personName: entry.personName,
      changeLabel: `${entry.oldRole ?? 'none'} → ${entry.newRole}`,
      reason: entry.reason,
      actorName: entry.changedByName,
    }))

    const statusRows: Row[] = statusQuery.data.map((entry) => ({
      id: `status-${entry.id}`,
      kind: 'STATUS',
      timestamp: entry.timestamp,
      personName: entry.personName,
      changeLabel: entry.active ? 'Reactivated' : 'Deactivated',
      reason: entry.reason,
      actorName: entry.changedByName,
    }))

    const departmentRows: Row[] = departmentQuery.data.content.map((entry) => ({
      id: `department-${entry.id}`,
      kind: 'DEPARTMENT',
      timestamp: entry.timestamp,
      departmentName: entry.departmentName,
      actorName: entry.performedByName,
    }))

    return [...taskRows, ...roleRows, ...statusRows, ...departmentRows].sort(
      (a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime(),
    )
  }, [taskQuery.data, roleQuery.data, statusQuery.data, departmentQuery.data])

  if (!isDirector) {
    return <Navigate to={ROUTES.dashboard} replace />
  }

  const isLoading = taskQuery.isLoading || roleQuery.isLoading || statusQuery.isLoading || departmentQuery.isLoading
  const error = taskQuery.error ?? roleQuery.error ?? statusQuery.error ?? departmentQuery.error
  const filteredRows = rows?.filter((row) => filter === 'ALL' || row.kind === filter)
  const totalPages = filteredRows ? Math.ceil(filteredRows.length / PAGE_SIZE) : 0
  const pageRows = filteredRows?.slice(page * PAGE_SIZE, page * PAGE_SIZE + PAGE_SIZE)

  return (
    <>
      <PageHeader
        breadcrumb="Throughline"
        title="Activity"
        right={
          <SegmentedControl
            options={FILTER_OPTIONS}
            value={filter}
            onChange={(next) => {
              setFilter(next)
              setPage(0)
            }}
            aria-label="Filter activity"
          />
        }
      />
      <Card>
        {isLoading ? (
          <LoadingSpinner />
        ) : error ? (
          <ErrorMessage message={error.message} />
        ) : !pageRows || pageRows.length === 0 ? (
          <EmptyState title="No activity yet" />
        ) : (
          <div>
            {pageRows.map((row) =>
              row.kind === 'TASK' ? (
                <div key={row.id} className={styles.row}>
                  <div className={styles.titleCol}>
                    <span className={styles.taskCode}>{row.taskCode}</span>
                    <span className={styles.title}>{row.title}</span>
                    {row.parentTaskCode && (
                      <span className={styles.subtext}>under {row.parentTaskTitle ?? row.parentTaskCode}</span>
                    )}
                  </div>
                  <span className={row.action === 'CREATED' ? styles.created : styles.deleted}>{row.action}</span>
                  <span className={styles.assignee}>{row.assigneeSummary}</span>
                  <span className={styles.actor}>by {row.actorName}</span>
                  <span className={styles.timestamp}>{formatDateTime(row.timestamp)}</span>
                </div>
              ) : row.kind === 'DEPARTMENT' ? (
                <div key={row.id} className={styles.row}>
                  <div className={styles.titleCol}>
                    <span className={styles.title}>{row.departmentName}</span>
                  </div>
                  <span className={styles.deleted}>DELETED</span>
                  <span className={styles.assignee} />
                  <span className={styles.actor}>by {row.actorName}</span>
                  <span className={styles.timestamp}>{formatDateTime(row.timestamp)}</span>
                </div>
              ) : (
                <div key={row.id} className={styles.row}>
                  <div className={styles.titleCol}>
                    <span className={styles.title}>{row.personName}</span>
                    {row.reason && <span className={styles.subtext}>{row.reason}</span>}
                  </div>
                  <span className={row.kind === 'ROLE' ? styles.roleChange : styles.statusChange}>{row.changeLabel}</span>
                  <span className={styles.assignee} />
                  <span className={styles.actor}>by {row.actorName}</span>
                  <span className={styles.timestamp}>{formatDateTime(row.timestamp)}</span>
                </div>
              ),
            )}
          </div>
        )}
      </Card>
      {rows && (
        <div className={styles.pagination}>
          <Pagination page={page} totalPages={totalPages} onChange={setPage} />
        </div>
      )}
    </>
  )
}
