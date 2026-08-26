import { useMediaQuery } from '../../../hooks/useMediaQuery'
import { EmptyState } from '../../../components/ui/EmptyState'
import type { TaskListItem } from '../../../types/task.types'
import { TaskTableHeader } from './TaskTableHeader'
import { TaskTableRow } from './TaskTableRow'
import { TaskMobileRow } from './TaskMobileRow'
import styles from './TaskTable.module.css'

/** Table shell. Below 768px, rows render as TaskMobileRow instead — no duplicated page. */
export function TaskTable({ tasks }: { tasks: TaskListItem[] }) {
  const isMobile = useMediaQuery('(max-width: 768px)')

  if (tasks.length === 0) {
    return <EmptyState title="No tasks match this view" description="Try a different status filter or search." />
  }

  if (isMobile) {
    return (
      <div>
        {tasks.map((task) => (
          <TaskMobileRow key={task.id} task={task} />
        ))}
      </div>
    )
  }

  return (
    <div className={styles.table}>
      <TaskTableHeader />
      {tasks.map((task) => (
        <TaskTableRow key={task.id} task={task} />
      ))}
    </div>
  )
}
