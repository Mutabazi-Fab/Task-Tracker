import { Link } from 'react-router-dom'
import { ROUTES } from '../../../app/routes'
import { ProgressBar } from '../../../components/ui/ProgressBar'
import { StatusChip } from '../../../components/ui/StatusChip'
import { formatPercentage } from '../../../lib/formatPercentage'
import type { TaskListItem } from '../../../types/task.types'
import { TaskLastCommentCell } from './TaskLastCommentCell'
import styles from './TaskMobileRow.module.css'

/** Stacked row for narrow screens — replaces TaskTableRow below 768px. */
export function TaskMobileRow({ task }: { task: TaskListItem }) {
  return (
    <Link to={ROUTES.taskDetail(task.id)} className={styles.row}>
      <div className={styles.top}>
        <span className={styles.code}>{task.taskCode}</span>
        <StatusChip status={task.status} />
      </div>
      <span className={styles.title}>{task.title}</span>
      <span className={styles.assignee}>{task.assigneeName}</span>
      <div className={styles.progress}>
        <ProgressBar percentage={task.progressPercentage} status={task.status} />
        <span className={styles.percentage}>{formatPercentage(task.progressPercentage)}</span>
      </div>
      <TaskLastCommentCell comment={task.lastComment} />
    </Link>
  )
}
