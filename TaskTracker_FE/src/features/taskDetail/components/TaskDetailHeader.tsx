import { StatusChip } from '../../../components/ui/StatusChip'
import type { TaskDetail } from '../../../types/task.types'
import styles from './TaskDetailHeader.module.css'

/** Code, status chip, title, description. */
export function TaskDetailHeader({ task }: { task: TaskDetail }) {
  return (
    <div className={styles.wrap}>
      <div className={styles.top}>
        <span className={styles.code}>{task.taskCode}</span>
        <StatusChip status={task.status} />
      </div>
      <h2 className={styles.title}>{task.title}</h2>
      {task.description && <p className={styles.description}>{task.description}</p>}
    </div>
  )
}
