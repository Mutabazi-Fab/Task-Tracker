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
 *  "all tasks" the way it does for a Director/Super Admin. assignedPersonId scopes every
 *  query on this page (list, lanes, and search) the same way; there's no client-side
 *  filtering of a wider result set, since that would still ship the wider set to them. */
export function TaskListPage() {
  const { currentUser, isDirector } = useAuth()
  const [status, setStatus] = useState<TaskStatusFilterValue>('ALL')
  const [layout, setLayout] = useState<TaskLayout>('table')
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [createOpen, setCreateOpen] = useState(false)

  const scopeToPersonId = isDirector ? undefined : currentUser?.id
  const statusParam = status === 'ALL' ? undefined : status
  const tableQuery = useTasks({ status: statusParam, assignedPersonId: scopeToPersonId, page, size: PAGE_SIZE })
  const { searchQuery, debouncedQuery } = useTaskSearch(search, scopeToPersonId)
  // Keyed off the SAME debounced value the query itself is enabled/disabled on — see
  // useTaskSearch's doc comment for why using the raw `search` state here crashed
  // TaskTable during the ~300ms window before the debounce catches up.
  const isSearching = debouncedQuery.length > 0

  return (
    <>
      <PageHeader
        breadcrumb="Throughline"
        title={isDirector ? 'Tasks' : 'My Tasks'}
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
            placeholder={isDirector ? 'Search code or title…' : 'Search your tasks…'}
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
        <TaskLanesBoard assignedPersonId={scopeToPersonId} status={status} />
      )}

      {isDirector && <CreateTaskModal open={createOpen} onClose={() => setCreateOpen(false)} />}
    </>
  )
}
