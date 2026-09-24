import { useState } from 'react'
import { StatusChip } from '../../../components/ui/StatusChip'
import { EmptyState } from '../../../components/ui/EmptyState'
import { ErrorMessage } from '../../../components/ui/ErrorMessage'
import { LoadingSpinner } from '../../../components/ui/LoadingSpinner'
import { Pagination } from '../../../components/ui/Pagination'
import type { TaskSortValue, TaskStatus } from '../../../types/task.types'
import { useTasks } from '../hooks/useTasks'
import { TaskCard } from './TaskCard'
import styles from './TaskLaneColumn.module.css'

const LANE_PAGE_SIZE = 8

interface TaskLaneColumnProps {
  status: TaskStatus
  /** Same scoping as the rest of this page — undefined for a Director (sees everyone),
   *  the viewer's own id for a Member. */
  assignedPersonId?: number
  sort: TaskSortValue
  /** 'column' (default) for the normal three-lanes-side-by-side board — one narrow vertical stack of
   *  cards. */
  layout?: 'column' | 'grid'
}

/** One status column — owns its own paginated query and page state, independent of the other two
 *  columns. */
export function TaskLaneColumn({ status, assignedPersonId, sort, layout = 'column' }: TaskLaneColumnProps) {
  const [page, setPage] = useState(0)
  const query = useTasks({ status, assignedPersonId, page, size: LANE_PAGE_SIZE, sort })

  return (
    <div className={styles.column}>
      <div className={styles.header}>
        <StatusChip status={status} />
        <span className={styles.count}>{query.data?.totalElements ?? '…'}</span>
      </div>

      {query.isLoading ? (
        <LoadingSpinner />
      ) : query.isError ? (
        <ErrorMessage message={query.error.message} />
      ) : (
        <>
          <div className={layout === 'grid' ? styles.cardsGrid : styles.cards}>
            {query.data.content.length === 0 ? (
              <EmptyState title="Nothing here" />
            ) : (
              query.data.content.map((task) => <TaskCard key={task.id} task={task} />)
            )}
          </div>
          <Pagination page={page} totalPages={query.data.totalPages} onChange={setPage} />
        </>
      )}
    </div>
  )
}
