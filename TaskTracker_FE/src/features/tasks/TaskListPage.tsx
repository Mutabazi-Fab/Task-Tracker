import { useEffect, useState } from 'react'
import { PageHeader } from '../../components/layout/PageHeader'
import { Button } from '../../components/ui/Button'
import { Pagination } from '../../components/ui/Pagination'
import { TextField } from '../../components/ui/TextField'
import { QueryBoundary } from '../../components/feedback/QueryBoundary'
import { useAuth } from '../auth/useAuth'
import { useMarkCategoryRead } from '../notifications/hooks/useMarkCategoryRead'
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

/** A Member only ever sees tasks assigned directly to them — assignedPersonId scopes every
 *  query on this page (list, lanes, search), no client-side filtering of a wider set. A
 *  plain Director isn't unrestricted either: leaving assignedPersonId unset makes the
 *  backend fall back to their own department (see TaskController.departmentScopeForViewer),
 *  resolved server-side off the JWT. Only Executive/Super Admin see everything.
 *  isPlainDirector exists purely for the page's own copy (title/placeholder). */
export function TaskListPage() {
  const { currentUser, isDirector, isExecutive } = useAuth()
  const [status, setStatus] = useState<TaskStatusFilterValue>('ALL')
  const [layout, setLayout] = useState<TaskLayout>('table')
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [createOpen, setCreateOpen] = useState(false)
  const markCategoryRead = useMarkCategoryRead()

  // Clears the Sidebar's Tasks badge — every logged-in person can get this one (Member
  // included), so unlike Teams/Departments/Activity this runs unconditionally.
  useEffect(() => {
    markCategoryRead.mutate(['TASK_ASSIGNED', 'SUBTASK_ASSIGNED', 'TASK_REASSIGNED', 'SUBTASK_REASSIGNED'])
  }, [])

  const isPlainDirector = isDirector && !isExecutive
  const scopeToPersonId = isDirector ? undefined : currentUser?.id
  const statusParam = status === 'ALL' ? undefined : status
  // 'createdAt,desc' rather than 'none' — pinned tasks still float to the top either way
  // (see TaskServiceImpl.withPinnedFirst), but this makes the rest genuinely newest-first
  // instead of undefined database order.
  const tableQuery = useTasks({ status: statusParam, assignedPersonId: scopeToPersonId, page, size: PAGE_SIZE, sort: 'createdAt,desc' })
  const { searchQuery, debouncedQuery } = useTaskSearch(search, scopeToPersonId)
  // Keyed off the same debounced value the query is enabled/disabled on — see
  // useTaskSearch's doc comment for why the raw `search` state crashed TaskTable mid-debounce.
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
