import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { PageHeader } from '../../components/layout/PageHeader'
import { Card } from '../../components/ui/Card'
import { Button } from '../../components/ui/Button'
import { QueryBoundary } from '../../components/feedback/QueryBoundary'
import { ROUTES } from '../../app/routes'
import { useAuth } from '../auth/useAuth'
import { useTaskDetail } from './hooks/useTaskDetail'
import { TaskDetailHeader } from './components/TaskDetailHeader'
import { TaskProgressPanel } from './components/TaskProgressPanel'
import { TaskProgressSparkline } from './components/TaskProgressSparkline'
import { AssignmentMetaPanel } from './components/AssignmentMetaPanel'
import { AddCommentForm } from './components/AddCommentForm'
import { CommentTimeline } from './components/CommentTimeline'
import { ReassignmentHistoryPanel } from './components/ReassignmentHistoryPanel'
import { ReassignTaskModal } from './components/ReassignTaskModal'
import { SubtasksPanel } from './components/SubtasksPanel'
import { DeleteTaskModal } from './components/DeleteTaskModal'
import styles from './TaskDetailPage.module.css'

/** Composes the sections below. No data-fetching or layout logic of its own. */
export function TaskDetailPage() {
  const { taskId } = useParams<{ taskId: string }>()
  const query = useTaskDetail(Number(taskId))
  const { isDirector, currentUser } = useAuth()
  const navigate = useNavigate()
  const [reassignOpen, setReassignOpen] = useState(false)
  const [deleteOpen, setDeleteOpen] = useState(false)

  return (
    <QueryBoundary query={query}>
      {(task) => {
        const isTopLevel = task.parentTaskId === null
        // Mirrors the backend check in TaskServiceImpl.deleteTask: Director/Super Admin
        // only, the same authority that creates a top-level task — a Team Leader can't
        // delete even their own team's tasks (unlike reassign, just below).
        const canDelete = isDirector
        // Mirrors the backend check in TaskServiceImpl.requireCanReassign: only a
        // Director/Super Admin, or the Team Leader of the team actually responsible for
        // this task, may reassign it — an ordinary team member cannot.
        const canReassign =
          isDirector ||
          (task.owningTeamId !== null &&
            currentUser?.teams.some((t) => t.teamId === task.owningTeamId && t.isLeader))

        return (
          <>
            <PageHeader
              breadcrumb="Throughline / Tasks"
              title={task.taskCode}
              right={
                <div className={styles.headerActions}>
                  {canReassign && (
                    <Button variant="secondary" onClick={() => setReassignOpen(true)}>
                      Reassign
                    </Button>
                  )}
                  {canDelete && (
                    <Button variant="danger" onClick={() => setDeleteOpen(true)}>
                      Delete
                    </Button>
                  )}
                </div>
              }
            />

            <TaskDetailHeader task={task} />

            <div className={styles.progressRow}>
              <TaskProgressPanel percentage={task.progressPercentage} status={task.status} />
              <Card>
                <span className={styles.sparklineLabel}>Trend</span>
                <TaskProgressSparkline points={task.progressTimeline} />
              </Card>
            </div>

            <AssignmentMetaPanel task={task} />

            {isTopLevel && (
              <Card>
                <SubtasksPanel task={task} />
              </Card>
            )}

            <AddCommentForm taskId={task.id} />

            <Card>
              <span className={styles.sectionHeading}>Comment history</span>
              <CommentTimeline comments={task.comments} />
            </Card>

            <Card>
              <span className={styles.sectionHeading}>Reassignment history</span>
              <ReassignmentHistoryPanel reassignments={task.reassignments} />
            </Card>

            <ReassignTaskModal task={task} open={reassignOpen} onClose={() => setReassignOpen(false)} />

            {canDelete && (
              <DeleteTaskModal
                taskId={task.id}
                taskCode={task.taskCode}
                hasSubtasks={task.subtasks.length > 0}
                open={deleteOpen}
                onClose={() => setDeleteOpen(false)}
                onDeleted={() =>
                  navigate(task.parentTaskId !== null ? ROUTES.taskDetail(task.parentTaskId) : ROUTES.tasks, {
                    replace: true,
                  })
                }
              />
            )}
          </>
        )
      }}
    </QueryBoundary>
  )
}
