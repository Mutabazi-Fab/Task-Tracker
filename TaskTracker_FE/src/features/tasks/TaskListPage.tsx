import { useState } from 'react'
import { PageHeader } from '../../components/layout/PageHeader'
import { Button } from '../../components/ui/Button'
import { TextField } from '../../components/ui/TextField'
import { QueryBoundary } from '../../components/feedback/QueryBoundary'
import { useTasks } from './hooks/useTasks'
import { useTaskSearch } from './hooks/useTaskSearch'
import { TaskStatusFilter, type TaskStatusFilterValue } from './components/TaskStatusFilter'
import { TaskLayoutToggle, type TaskLayout } from './components/TaskLayoutToggle'
import { TaskTable } from './components/TaskTable'
import { TaskLanesBoard } from './components/TaskLanesBoard'
import { CreateTaskModal } from './components/CreateTaskModal'
import styles from './TaskListPage.module.css'

const PAGE_SIZE = 10
const LANES_SIZE = 200

export function TaskListPage() {
  const [status, setStatus] = useState<TaskStatusFilterValue>('ALL')
  const [layout, setLayout] = useState<TaskLayout>('table')
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [createOpen, setCreateOpen] = useState(false)

  const statusParam = status === 'ALL' ? undefined : status
  const tableQuery = useTasks({ status: statusParam, page, size: PAGE_SIZE })
  const lanesQuery = useTasks({ page: 0, size: LANES_SIZE })
  const searchQuery = useTaskSearch(search)
  const isSearching = search.trim().length > 0

  return (
    <>
      <PageHeader
        breadcrumb="Throughline"
        title="Tasks"
        right={<Button onClick={() => setCreateOpen(true)}>New task</Button>}
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
          <TextField value={search} onChange={setSearch} placeholder="Search code or title…" aria-label="Search tasks" />
          <TaskLayoutToggle value={layout} onChange={setLayout} />
        </div>
      </div>

      {isSearching ? (
        <QueryBoundary query={searchQuery}>{(results) => <TaskTable tasks={results} />}</QueryBoundary>
      ) : layout === 'table' ? (
        <>
          <QueryBoundary query={tableQuery}>{(result) => <TaskTable tasks={result.content} />}</QueryBoundary>
          {tableQuery.data && tableQuery.data.totalPages > 1 && (
            <div className={styles.pagination}>
              <Button variant="secondary" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                Previous
              </Button>
              <span className={styles.pageIndicator}>
                Page {page + 1} of {tableQuery.data.totalPages}
              </span>
              <Button
                variant="secondary"
                disabled={page + 1 >= tableQuery.data.totalPages}
                onClick={() => setPage((p) => p + 1)}
              >
                Next
              </Button>
            </div>
          )}
        </>
      ) : (
        <QueryBoundary query={lanesQuery}>{(result) => <TaskLanesBoard tasks={result.content} />}</QueryBoundary>
      )}

      <CreateTaskModal open={createOpen} onClose={() => setCreateOpen(false)} />
    </>
  )
}
