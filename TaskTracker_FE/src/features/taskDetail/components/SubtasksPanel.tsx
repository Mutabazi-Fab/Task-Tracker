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

/** Rendered for any task that can still be broken down further: a TEAM-assigned task under
 *  depth 2 (leaf-subtask flow, scoped to that team's roster), or a DEPARTMENT-assigned task
 *  (the implementation-task case, team-or-individual, org-wide) — see TaskDetailPage's
 *  canHaveSubtasks, mirrored exactly so this is never rendered with nothing to offer. A
 *  Department task can have more than one implementation task under it (parallel teams on
 *  the same mandate); this only ever adds a child, never converts the Department task
 *  itself. "Add subtask" is shown to a Director/Super Admin, the relevant Team Leader, or
 *  (Department task) that Department's head Director. The CEO seat (EXECUTIVE) never gets
 *  it — turning her Department task into real work is the Department's own Director's
 *  call; Super Admin keeps it as an unrestricted system-governance seat. */
export function SubtasksPanel({ task }: { task: TaskDetail }) {
  const { currentUser, isDirector, isExecutive } = useAuth()
  const [createOpen, setCreateOpen] = useState(false)

  const isCeo = currentUser?.role === 'EXECUTIVE'
  const isDepartmentTask = task.assigneeType === 'DEPARTMENT'
  const departmentQuery = useDepartment(isDepartmentTask ? (task.assigneeId ?? NaN) : NaN)

  const isThisTeamsLeader = currentUser?.teams.some((t) => t.teamId === task.assigneeId && t.isLeader)
  const isThisDepartmentsHead =
    isDepartmentTask && departmentQuery.data?.headDirectorId === currentUser?.id
  // Mirrors TaskServiceImpl.createLeafSubtask: a plain Director must head THIS task's own
  // department, not merely outrank a Member. Compared via taskDepartmentId against the
  // viewer's own departmentId, same simplifying assumption used across this app.
  const isPlainDirector = isDirector && !isExecutive
  const headsThisTasksDepartment =
    isPlainDirector && task.taskDepartmentId !== null && task.taskDepartmentId === currentUser?.departmentId

  const canCreate = isDepartmentTask
    ? (isExecutive && !isCeo) || isThisDepartmentsHead
    : (isExecutive && !isCeo) || headsThisTasksDepartment || isThisTeamsLeader

  return (
    <>
      <div className={styles.header}>
        <span className={styles.title}>{isDepartmentTask ? 'Implementation task' : 'Subtasks'}</span>
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
          // Whatever source is already recorded on THIS task — the new child inherits and
          // locks it (see CreateSubtaskForm) rather than re-asking for something on record.
          parentSource={task.source}
          parentSourceLabel={task.sourceLabel}
          open={createOpen}
          onClose={() => setCreateOpen(false)}
        />
      )}
    </>
  )
}
