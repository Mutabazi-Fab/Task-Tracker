import { Icon } from '../../../components/ui/Icon'
import { SeverityBadge } from '../../../components/ui/SeverityBadge'
import { StatusChip } from '../../../components/ui/StatusChip'
import type { TaskDetail } from '../../../types/task.types'
import styles from './TaskDetailHeader.module.css'

/** Code, status chip, severity badge (if set), pin indicator (if pinned), title,
 *  description. The "New" badge lives up in PageHeader's title row now, not here — see
 *  TaskDetailPage. */
export function TaskDetailHeader({ task }: { task: TaskDetail }) {
  return (
    <div className={styles.wrap}>
      <div className={styles.top}>
        <span className={styles.code}>{task.taskCode}</span>
        {task.pinned && <Icon name="pin" size={14} className={styles.pinIcon} />}
        <StatusChip status={task.status} />
        {task.severity && <SeverityBadge severity={task.severity} />}
      </div>
      <h2 className={styles.title}>{task.title}</h2>
      {task.description && <p className={styles.description}>{task.description}</p>}
      {task.source && (
        <p className={styles.source}>
          Source: {task.source}
          {task.sourceLabel ? ` — ${task.sourceLabel}` : ''}
        </p>
      )}
    </div>
  )
}
