import styles from './TaskTable.module.css'

export function TaskTableHeader() {
  return (
    <div className={styles.headerRow}>
      <span>Code</span>
      <span>Title</span>
      <span>Assignee</span>
      <span>Progress</span>
      <span>Status</span>
      <span>Last comment</span>
    </div>
  )
}
