import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Card } from '../../../components/ui/Card'
import { EmptyState } from '../../../components/ui/EmptyState'
import { Pagination } from '../../../components/ui/Pagination'
import { SegmentedControl } from '../../../components/ui/SegmentedControl'
import { QueryBoundary } from '../../../components/feedback/QueryBoundary'
import { ROUTES } from '../../../app/routePaths'
import { TaskCard } from '../../tasks/components/TaskCard'
import { TaskSortToggle } from '../../tasks/components/TaskSortToggle'
import { useAuth } from '../../auth/useAuth'
import { useDirectorTasks } from '../hooks/useDirectorTasks'
import { useDirectorCriticalAndCeoAssignedTasks } from '../hooks/useDirectorCriticalAndCeoAssignedTasks'
import type { TaskSortValue } from '../../../types/task.types'
import styles from './TopLevelTasksSection.module.css'

const PAGE_SIZE = 12

type Tab = 'combined' | 'mine'

const TAB_OPTIONS: { label: string; value: Tab }[] = [
  { label: 'Critical & CEO-assigned', value: 'combined' },
  { label: 'My initiatives', value: 'mine' },
]

const EMPTY_STATE: Record<Tab, { title: string; description: string }> = {
  combined: {
    title: 'Nothing flagged',
    description: 'HIGH/CRITICAL tasks and anything the CEO assigns directly in your department show up here.',
  },
  mine: { title: 'No initiatives yet', description: 'Top-level tasks you create show up here.' },
}

/** A plain Director's dashboard equivalent of the Executive's "Critical & CEO-assigned" panel — one
 *  panel, toggling between two department-scoped views, rather than two (or three) separate panels
 *  stacked on the page. */
export function DirectorTasksPanel() {
  const { currentUser } = useAuth()
  const [tab, setTab] = useState<Tab>('combined')
  const [page, setPage] = useState(0)
  const [sort, setSort] = useState<TaskSortValue>('updatedAt,desc')

  const combinedQuery = useDirectorCriticalAndCeoAssignedTasks(page, PAGE_SIZE, sort, tab === 'combined')
  const mineQuery = useDirectorTasks(tab === 'mine' ? (currentUser?.id ?? NaN) : NaN, page, PAGE_SIZE, sort)
  const query = tab === 'combined' ? combinedQuery : mineQuery
  const emptyState = EMPTY_STATE[tab]

  function switchTab(next: Tab) {
    setTab(next)
    setPage(0)
  }

  return (
    <Card>
      <div className={styles.headingRow}>
        <SegmentedControl options={TAB_OPTIONS} value={tab} onChange={switchTab} aria-label="Task panel" />
        <div className={styles.controls}>
          <Link to={ROUTES.tasks} className={styles.viewAllLink}>
            View every task →
          </Link>
          <TaskSortToggle
            value={sort}
            onChange={(next) => {
              setSort(next)
              setPage(0)
            }}
          />
        </div>
      </div>
      <QueryBoundary query={query}>
        {(result) =>
          result.content.length === 0 ? (
            <EmptyState title={emptyState.title} description={emptyState.description} />
          ) : (
            <>
              <div className={styles.grid}>
                {result.content.map((task) => (
                  <TaskCard key={task.id} task={task} />
                ))}
              </div>
              <div className={styles.pagination}>
                <Pagination page={page} totalPages={result.totalPages} onChange={setPage} />
              </div>
            </>
          )
        }
      </QueryBoundary>
    </Card>
  )
}
