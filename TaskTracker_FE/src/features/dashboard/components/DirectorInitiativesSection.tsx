import { useState } from 'react'
import { Card } from '../../../components/ui/Card'
import { EmptyState } from '../../../components/ui/EmptyState'
import { Pagination } from '../../../components/ui/Pagination'
import { QueryBoundary } from '../../../components/feedback/QueryBoundary'
import { TaskCard } from '../../tasks/components/TaskCard'
import { useAuth } from '../../auth/useAuth'
import { useDirectorTasks } from '../hooks/useDirectorTasks'
import styles from './DirectorInitiativesSection.module.css'

const PAGE_SIZE = 12

/**
 * Only rendered for a Director/Super Admin (see DashboardPage) — the Director's Dashboard
 * default view from the spec: only the top-level tasks THIS Director created, each
 * already showing its auto-calculated % (rolled up from subtasks) via TaskCard.
 *
 * Paginated server-side (the backend endpoint already accepts a Pageable) at 12 per page —
 * roughly two to three rows on this grid's minmax(220px, 1fr) columns depending on viewport
 * width, so the dashboard never grows an unbounded scroll as a Director accumulates
 * initiatives.
 */
export function DirectorInitiativesSection() {
  const { currentUser } = useAuth()
  const [page, setPage] = useState(0)
  const query = useDirectorTasks(currentUser?.id ?? NaN, page, PAGE_SIZE)

  return (
    <Card>
      <div className={styles.sectionHeading}>My initiatives</div>
      <QueryBoundary query={query}>
        {(result) =>
          result.content.length === 0 ? (
            <EmptyState title="No initiatives yet" description="Top-level tasks you create show up here." />
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
