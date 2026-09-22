import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Card } from '../../../components/ui/Card'
import { EmptyState } from '../../../components/ui/EmptyState'
import { Pagination } from '../../../components/ui/Pagination'
import { QueryBoundary } from '../../../components/feedback/QueryBoundary'
import { ROUTES } from '../../../app/routePaths'
import { TaskCard } from '../../tasks/components/TaskCard'
import { TaskSortToggle } from '../../tasks/components/TaskSortToggle'
import { useAuth } from '../../auth/useAuth'
import { useDirectorTasks } from '../hooks/useDirectorTasks'
import { useExecutiveTasks } from '../hooks/useExecutiveTasks'
import type { TaskSortValue } from '../../../types/task.types'
import styles from './TopLevelTasksSection.module.css'

const PAGE_SIZE = 12

interface TopLevelTasksSectionProps {
  /** 'mine' — top-level tasks the logged-in Director created. 'org-wide' — every
   *  CRITICAL-severity task at any depth, plus every task an Executive/Super Admin
   *  personally assigned (see useExecutiveTasks). Executive/Super Admin only; a plain
   *  Director's equivalent lives in DirectorTasksPanel instead. */
  scope: 'mine' | 'org-wide'
}

/** Paginated server-side at 12 per page so the dashboard never grows an unbounded scroll
 *  as tasks accumulate. Each card shows its auto-calculated % via TaskCard. */
export function TopLevelTasksSection({ scope }: TopLevelTasksSectionProps) {
  const { currentUser } = useAuth()
  const [page, setPage] = useState(0)
  const [sort, setSort] = useState<TaskSortValue>('updatedAt,desc')

  const mineQuery = useDirectorTasks(scope === 'mine' ? (currentUser?.id ?? NaN) : NaN, page, PAGE_SIZE, sort)
  const orgWideQuery = useExecutiveTasks(page, PAGE_SIZE, sort, scope === 'org-wide')
  const query = scope === 'mine' ? mineQuery : orgWideQuery

  // Org-wide is "what actually needs an Executive's eyes" — not depth-based. The literal
  // everything view already exists at the Tasks page, linked below.
  const heading = scope === 'mine' ? 'My initiatives' : 'Critical & CEO-assigned'

  return (
    <Card>
      <div className={styles.headingRow}>
        <div className={styles.sectionHeading}>{heading}</div>
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
            <EmptyState
              title={scope === 'mine' ? 'No initiatives yet' : 'Nothing flagged'}
              description={
                scope === 'mine'
                  ? 'Top-level tasks you create show up here.'
                  : 'CRITICAL-severity tasks and anything you assign directly show up here — everything else lives on the Tasks page.'
              }
            />
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
