import { Link } from 'react-router-dom'
import { ROUTES } from '../../../app/routes'
import { ProgressBar } from '../../../components/ui/ProgressBar'
import { StatusChip } from '../../../components/ui/StatusChip'
import { formatDate } from '../../../lib/formatDate'
import { formatPercentage } from '../../../lib/formatPercentage'
import type { TaskListItem } from '../../../types/task.types'
import { TaskLastCommentCell } from './TaskLastCommentCell'
import styles from './TaskTable.module.css'

/** ONE row: code, title, assignee, progress, last comment. */
export function TaskTableRow({ task }: { task: TaskListItem }) {
  return (
    <Link to={ROUTES.taskDetail(task.id)} className={styles.row}>
      <span className={styles.code}>{task.taskCode}</span>
      <div className={styles.title}>
        <span className={styles.titleText}>{task.title}</span>
        <span className={styles.dateAssigned}>{formatDate(task.dateAssigned)}</span>
      </div>
      <div className={styles.assignee}>
        <span className={styles.assigneeName}>{task.assigneeName}</span>
        {/* A subtask and a standalone individual task both carry assigneeType
            INDIVIDUAL — parentTaskCode is the only thing that actually tells them
            apart, so it takes over this line instead of just repeating "INDIVIDUAL". */}
        <span className={styles.assigneeType}>
          {task.parentTaskCode ? `SUBTASK · under ${task.parentTaskCode}` : task.assigneeType}
        </span>
      </div>
      <div className={styles.progress}>
        <ProgressBar percentage={task.progressPercentage} status={task.status} />
        <span className={styles.percentage}>{formatPercentage(task.progressPercentage)}</span>
      </div>
      <span>
        <StatusChip status={task.status} />
      </span>
      <TaskLastCommentCell comment={task.lastComment} />
    </Link>
  )
}
