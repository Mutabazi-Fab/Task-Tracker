import { Link } from 'react-router-dom'
import { ROUTES } from '../../../app/routes'
import { Card } from '../../../components/ui/Card'
import { Icon } from '../../../components/ui/Icon'
import { ProgressBar } from '../../../components/ui/ProgressBar'
import { SeverityBadge } from '../../../components/ui/SeverityBadge'
import { formatPercentage } from '../../../lib/formatPercentage'
import type { TaskListItem } from '../../../types/task.types'
import { TaskLastCommentCell } from './TaskLastCommentCell'
import styles from './TaskCard.module.css'

/** One card in a lane. */
export function TaskCard({ task }: { task: TaskListItem }) {
  return (
    <Link to={ROUTES.taskDetail(task.id)} className={styles.link}>
      <Card padding="sm">
        <div className={styles.top}>
          <span className={styles.code}>{task.taskCode}</span>
          <div className={styles.badges}>
            {task.pinned && <Icon name="pin" size={12} className={styles.pinIcon} />}
            {task.severity && <SeverityBadge severity={task.severity} />}
            <span className={styles.percentage}>{formatPercentage(task.progressPercentage)}</span>
          </div>
        </div>
        <p className={styles.title}>{task.title}</p>
        <p className={styles.assignee}>
          {task.assigneeName}
          {/* Same distinction as the Table view — without this, a subtask and a
              standalone individual task are visually identical here. */}
          {task.parentTaskCode && <span className={styles.subtaskTag}> · under {task.parentTaskCode}</span>}
        </p>
        <ProgressBar percentage={task.progressPercentage} status={task.status} />
        <div className={styles.comment}>
          <TaskLastCommentCell comment={task.lastComment} />
        </div>
      </Card>
    </Link>
  )
}
