import { useState } from 'react'
import { PageHeader } from '../../components/layout/PageHeader'
import { Button } from '../../components/ui/Button'
import { Pagination } from '../../components/ui/Pagination'
import { TextField } from '../../components/ui/TextField'
import { QueryBoundary } from '../../components/feedback/QueryBoundary'
import { useAuth } from '../auth/useAuth'
import { useTasks } from './hooks/useTasks'
import { useTaskSearch } from './hooks/useTaskSearch'
import { TaskStatusFilter, type TaskStatusFilterValue } from './components/TaskStatusFilter'
import { TaskLayoutToggle, type TaskLayout } from './components/TaskLayoutToggle'
import { TaskTable } from './components/TaskTable'
import { TaskStatusSummary } from './components/TaskStatusSummary'
import { TaskLanesBoard } from './components/TaskLanesBoard'
import { CreateTaskModal } from './components/CreateTaskModal'
import styles from './TaskListPage.module.css'

const PAGE_SIZE = 10

/** A Member only ever sees tasks assigned directly to them — this page never shows them
 *  "all tasks" the way it does for a Director/Executive/Super Admin. assignedPersonId
 *  scopes every query on this page (list, lanes, and search) the same way; there's no
 *  client-side filtering of a wider result set, since that would still ship the wider set
 *  to them.
 *
 *  A plain Director isn't unrestricted any more either, just scoped differently: leaving
 *  assignedPersonId unset (same as before) now makes the backend fall back to their own
 *  department instead of "everything" — see TaskController.departmentScopeForViewer.
 *  Nothing needs passing from here for that; it's resolved server-side off the JWT. Only
 *  Executive/Super Admin still see literally every task. isPlainDirector below exists
 *  purely for the page's own copy (title/placeholder), so it's honest about that scope. */
export function TaskListPage() {
  const { currentUser, isDirector, isExecutive } = useAuth()
  const [status, setStatus] = useState<TaskStatusFilterValue>('ALL')
  const [layout, setLayout] = useState<TaskLayout>('table')
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [createOpen, setCreateOpen] = useState(false)

  const isPlainDirector = isDirector && !isExecutive
  const scopeToPersonId = isDirector ? undefined : currentUser?.id
  const statusParam = status === 'ALL' ? undefined : status
  // 'createdAt,desc' rather than 'none' — pinned tasks still float to the top either way
  // (the backend composes DESC-pinned as a leading sort key onto whatever's requested, see
  // TaskServiceImpl.withPinnedFirst), but 'none' left everything else in undefined database
  // order. This makes the rest genuinely newest-created-first, so a task made today lands
  // right after the pinned ones and ahead of yesterday's, and so on.
  const tableQuery = useTasks({ status: statusParam, assignedPersonId: scopeToPersonId, page, size: PAGE_SIZE, sort: 'createdAt,desc' })
  const { searchQuery, debouncedQuery } = useTaskSearch(search, scopeToPersonId)
  // Keyed off the SAME debounced value the query itself is enabled/disabled on — see
  // useTaskSearch's doc comment for why using the raw `search` state here crashed
  // TaskTable during the ~300ms window before the debounce catches up.
  const isSearching = debouncedQuery.length > 0

  return (
    <>
      <PageHeader
        breadcrumb="Throughline"
        title={isPlainDirector ? `${currentUser?.departmentName ?? 'Department'} Tasks` : isDirector ? 'Tasks' : 'My Tasks'}
        right={isDirector ? <Button onClick={() => setCreateOpen(true)}>New task</Button> : undefined}
      />

      <div className={styles.controls}>
        <TaskStatusFilter
          value={status}
          onChange={(next) => {
            setStatus(next)
            setPage(0)
          }}
        />
        <div className={styles.controlsRight}>
          <TextField
            value={search}
            onChange={setSearch}
            placeholder={isPlainDirector ? "Search your department's tasks…" : isDirector ? 'Search code or title…' : 'Search your tasks…'}
            aria-label="Search tasks"
          />
          <TaskLayoutToggle value={layout} onChange={setLayout} />
        </div>
      </div>

      {isSearching ? (
        <QueryBoundary query={searchQuery}>{(results) => <TaskTable tasks={results} />}</QueryBoundary>
      ) : layout === 'table' ? (
        <>
          <TaskStatusSummary assignedPersonId={scopeToPersonId} />
          <QueryBoundary query={tableQuery}>{(result) => <TaskTable tasks={result.content} />}</QueryBoundary>
          {tableQuery.data && (
            <div className={styles.pagination}>
              <Pagination page={page} totalPages={tableQuery.data.totalPages} onChange={setPage} />
            </div>
          )}
        </>
      ) : (
        <TaskLanesBoard assignedPersonId={scopeToPersonId} status={status} sort="createdAt,desc" />
      )}

      {isDirector && <CreateTaskModal open={createOpen} onClose={() => setCreateOpen(false)} />}
    </>
  )
}
