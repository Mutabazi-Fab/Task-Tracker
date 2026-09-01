import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ROUTES } from '../../../app/routes'
import { Button } from '../../../components/ui/Button'
import { EmptyState } from '../../../components/ui/EmptyState'
import { ProgressBar } from '../../../components/ui/ProgressBar'
import { StatusChip } from '../../../components/ui/StatusChip'
import { useAuth } from '../../auth/useAuth'
import { CreateSubtaskModal } from './CreateSubtaskModal'
import type { TaskDetail } from '../../../types/task.types'
import styles from './SubtasksPanel.module.css'

/**
 * Only rendered for a top-level task (task.parentTaskId === null) — a subtask can't have
 * subtasks of its own. "Add subtask" is shown to a Director/Super Admin, or to whoever
 * leads the team this task is assigned to (task.assigneeId is that team's id for a
 * top-level task) — the same two roles the backend itself allows to create one.
 */
export function SubtasksPanel({ task }: { task: TaskDetail }) {
  const { currentUser, isDirector } = useAuth()
  const [createOpen, setCreateOpen] = useState(false)

  const isThisTeamsLeader =
    task.assigneeId != null && currentUser?.teams.some((t) => t.teamId === task.assigneeId && t.isLeader)
  const canCreate = isDirector || isThisTeamsLeader

  return (
    <>
      <div className={styles.header}>
        <span>Subtasks</span>
        {canCreate && task.assigneeId != null && (
          <Button variant="secondary" onClick={() => setCreateOpen(true)}>
            Add subtask
          </Button>
        )}
      </div>

      {task.subtasks.length === 0 ? (
        <EmptyState title="No subtasks yet" />
      ) : (
        <div className={styles.list}>
          {task.subtasks.map((subtask) => (
            <Link key={subtask.id} to={ROUTES.taskDetail(subtask.id)} className={styles.row}>
              <div className={styles.titleCol}>
                <span className={styles.taskCode}>{subtask.taskCode}</span>
                <span className={styles.title}>{subtask.title}</span>
              </div>
              <span className={styles.assignee}>{subtask.assigneeName}</span>
              <span className={styles.createdByRole}>{subtask.createdByRole}</span>
              <div className={styles.progressCol}>
                <ProgressBar percentage={subtask.progressPercentage} status={subtask.status} />
                <span className={styles.progressValue}>{subtask.progressPercentage}%</span>
              </div>
              <StatusChip status={subtask.status} />
            </Link>
          ))}
        </div>
      )}

      {canCreate && task.assigneeId != null && (
        <CreateSubtaskModal
          parentTaskId={task.id}
          teamId={task.assigneeId}
          open={createOpen}
          onClose={() => setCreateOpen(false)}
        />
      )}
    </>
  )
}
