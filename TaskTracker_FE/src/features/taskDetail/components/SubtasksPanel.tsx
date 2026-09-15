import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ROUTES } from '../../../app/routes'
import { Button } from '../../../components/ui/Button'
import { EmptyState } from '../../../components/ui/EmptyState'
import { ProgressBar } from '../../../components/ui/ProgressBar'
import { StatusChip } from '../../../components/ui/StatusChip'
import { useAuth } from '../../auth/useAuth'
import { useDepartment } from '../../departments/hooks/useDepartment'
import { CreateSubtaskModal } from './CreateSubtaskModal'
import type { TaskDetail } from '../../../types/task.types'
import styles from './SubtasksPanel.module.css'

/**
 * Rendered for any task that can still be broken down further: a TEAM-assigned task under
 * depth 2 (a plain top-level task, or a depth-1 Department implementation task — either
 * way, its "Add subtask" flow is the ordinary leaf case, scoped to that team's roster), or
 * a DEPARTMENT-assigned task (always depth 0 — its "Add subtask" is the new implementation-
 * task case, team-or-individual, org-wide). See TaskDetailPage's canHaveSubtasks, which
 * mirrors this exactly so the panel is never rendered when it'd have nothing to offer.
 *
 * A Department task can have more than one implementation task under it (e.g. TSK-0001 has
 * both a Digital Banking and a Mobile Banking one, two teams working different slices of
 * the same CEO mandate in parallel) — this never converts the Department task itself, it
 * only ever adds a new child underneath it.
 *
 * "Add subtask" itself is shown to a Director/Super Admin, to whoever leads the team this
 * task is assigned to (the ordinary leaf case), or — for a Department task — to that
 * Department's own head Director or Super Admin.
 *
 * The CEO seat (role EXECUTIVE) never gets this button, on a Department task or anywhere
 * else: her job is handing work to a Department and then watching progress/commenting —
 * turning that into real team-or-individual work is the Department's own Director's call,
 * not hers. Super Admin is a separate, unrestricted system-governance seat and keeps it.
 */
export function SubtasksPanel({ task }: { task: TaskDetail }) {
  const { currentUser, isDirector, isExecutive } = useAuth()
  const [createOpen, setCreateOpen] = useState(false)

  const isCeo = currentUser?.role === 'EXECUTIVE'
  const isDepartmentTask = task.assigneeType === 'DEPARTMENT'
  const departmentQuery = useDepartment(isDepartmentTask ? (task.assigneeId ?? NaN) : NaN)

  const isThisTeamsLeader = currentUser?.teams.some((t) => t.teamId === task.assigneeId && t.isLeader)
  const isThisDepartmentsHead =
    isDepartmentTask && departmentQuery.data?.headDirectorId === currentUser?.id

  const canCreate = isDepartmentTask
    ? (isExecutive && !isCeo) || isThisDepartmentsHead
    : (isDirector && !isCeo) || isThisTeamsLeader

  return (
    <>
      <div className={styles.header}>
        <span>{isDepartmentTask ? 'Implementation task' : 'Subtasks'}</span>
        {canCreate && (
          <Button variant="primary" onClick={() => setCreateOpen(true)}>
            {isDepartmentTask ? 'Add implementation task' : 'Add subtask'}
          </Button>
        )}
      </div>

      {task.subtasks.length === 0 ? (
        <EmptyState title={isDepartmentTask ? 'No implementation task yet' : 'No subtasks yet'} />
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

      {canCreate && (
        <CreateSubtaskModal
          parentTaskId={task.id}
          teamId={task.assigneeId ?? NaN}
          isDepartmentImplementation={isDepartmentTask}
          departmentId={isDepartmentTask ? (task.assigneeId ?? undefined) : undefined}
          open={createOpen}
          onClose={() => setCreateOpen(false)}
        />
      )}
    </>
  )
}
