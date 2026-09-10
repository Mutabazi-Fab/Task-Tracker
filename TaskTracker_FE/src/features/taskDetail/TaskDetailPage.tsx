import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { PageHeader } from '../../components/layout/PageHeader'
import { Card } from '../../components/ui/Card'
import { Button } from '../../components/ui/Button'
import { QueryBoundary } from '../../components/feedback/QueryBoundary'
import { ROUTES } from '../../app/routes'
import { useAuth } from '../auth/useAuth'
import { useDepartment } from '../departments/hooks/useDepartment'
import { useTaskDetail } from './hooks/useTaskDetail'
import { useSetPinned } from './hooks/useSetPinned'
import { TaskDetailHeader } from './components/TaskDetailHeader'
import { TaskProgressPanel } from './components/TaskProgressPanel'
import { TaskProgressSparkline } from './components/TaskProgressSparkline'
import { AssignmentMetaPanel } from './components/AssignmentMetaPanel'
import { AddCommentForm } from './components/AddCommentForm'
import { CommentTimeline } from './components/CommentTimeline'
import { DiscussionPanel } from './components/DiscussionPanel'
import { ReassignmentHistoryPanel } from './components/ReassignmentHistoryPanel'
import { ReassignTaskModal } from './components/ReassignTaskModal'
import { RequestExtensionModal } from './components/RequestExtensionModal'
import { ExtendDeadlineModal } from './components/ExtendDeadlineModal'
import { DeadlineExtensionHistoryPanel } from './components/DeadlineExtensionHistoryPanel'
import { SubtasksPanel } from './components/SubtasksPanel'
import { DeleteTaskModal } from './components/DeleteTaskModal'
import type { TaskDetail } from '../../types/task.types'
import styles from './TaskDetailPage.module.css'

/** Thin wrapper — waits for the task, then hands it to TaskDetailBody. Every hook that
 *  needs the loaded task (in particular useDepartment, only relevant once we know
 *  assigneeType) lives in the child instead, so nothing here is called conditionally. */
export function TaskDetailPage() {
  const { taskId } = useParams<{ taskId: string }>()
  const query = useTaskDetail(Number(taskId))

  return <QueryBoundary query={query}>{(task) => <TaskDetailBody task={task} />}</QueryBoundary>
}

function TaskDetailBody({ task }: { task: TaskDetail }) {
  const { isDirector, isExecutive, currentUser } = useAuth()
  const navigate = useNavigate()
  const [reassignOpen, setReassignOpen] = useState(false)
  const [deleteOpen, setDeleteOpen] = useState(false)
  const [requestExtensionOpen, setRequestExtensionOpen] = useState(false)
  const [extendDeadlineOpen, setExtendDeadlineOpen] = useState(false)

  const isDepartmentAssigned = task.assigneeType === 'DEPARTMENT'
  const departmentQuery = useDepartment(isDepartmentAssigned ? (task.assigneeId ?? NaN) : NaN)
  const setPinned = useSetPinned(task.id)

  // A TEAM-assigned task can always be broken down further, as long as it's not already
  // at maximum depth (mirrors the backend's depth >= 2 rejection in
  // TaskServiceImpl.createSubtask) — this covers both a plain top-level team task AND a
  // depth-1 Department implementation task, the same "Add subtask" flow either way. A
  // DEPARTMENT-assigned task (always depth 0) gets the same panel in its other new shape:
  // creating its one implementation task. An INDIVIDUAL task (top-level or a leaf subtask)
  // has nobody behind it to break work down further — a dead end either way, so it gets no
  // Subtasks panel at all.
  const canHaveSubtasks = (task.assigneeType === 'TEAM' && task.depth < 2) || isDepartmentAssigned
  // Mirrors the backend check in TaskServiceImpl.deleteTask: Director/Super Admin only,
  // the same authority that creates a top-level task — a Team Leader can't delete even
  // their own team's tasks (unlike reassign, just below).
  const canDelete = isDirector
  // Mirrors the backend check in TaskServiceImpl.setPinned: Director-or-above, same tier
  // as delete — a manual, independently-editable toggle, not derived from severity.
  const canPin = isDirector
  // Mirrors the backend check in TaskServiceImpl.reassignTask: a Department-level task
  // moves to a different Department entirely, restricted to Executive/Super Admin — the
  // same authority that assigns one in the first place, not a Director who happens to
  // lead some unrelated team. Everything else follows requireCanReassign: a Director/
  // Super Admin, or the Team Leader of the team actually responsible for this task, may
  // reassign it.
  const canReassign = isDepartmentAssigned
    ? isExecutive
    : isDirector ||
      (task.owningTeamId !== null && (currentUser?.teams.some((t) => t.teamId === task.owningTeamId && t.isLeader) ?? false))

  // Mirrors TaskServiceImpl.isDeadlineOverrideTier: Executive-or-above for a Department
  // task, Director-or-above otherwise.
  const isDeadlineOverrideTier = isDepartmentAssigned ? isExecutive : isDirector
  // Mirrors TaskServiceImpl.resolveAccountablePerson: this task's own Team Leader,
  // individual assignee, or (a Department task) its head Director.
  const isAccountablePerson = isDepartmentAssigned
    ? departmentQuery.data?.headDirectorId === currentUser?.id
    : task.assigneeType === 'TEAM'
      ? (currentUser?.teams.some((t) => t.teamId === task.assigneeId && t.isLeader) ?? false)
      : task.assigneeId === currentUser?.id
  // Requesting an extension: this task's own accountable person, or the override tier.
  const canRequestExtension = isDeadlineOverrideTier || isAccountablePerson
  // Deciding a request, or extending directly: this task's own deadline decider — a
  // Director-or-above, chain-of-command resolved, NOT necessarily assignedById (a Team
  // Leader can be a leaf subtask's assignedBy but has no authority over its deadline) —
  // or the override tier. Mirrors TaskServiceImpl.requireCanDecideDeadline/resolveDeadlineDecider.
  const canDecideDeadline = isDeadlineOverrideTier || task.deadlineDeciderId === currentUser?.id

  return (
    <>
      <PageHeader
        breadcrumb="Throughline / Tasks"
        title={task.taskCode}
        right={
          <div className={styles.headerActions}>
            {canPin && (
              <Button
                variant="ghost"
                onClick={() => setPinned.mutate({ pinned: !task.pinned, changedById: currentUser?.id ?? 0 })}
                disabled={setPinned.isPending || !currentUser}
              >
                {task.pinned ? 'Unpin' : 'Pin'}
              </Button>
            )}
            {canRequestExtension && (
              <Button variant="secondary" onClick={() => setRequestExtensionOpen(true)}>
                Request extension
              </Button>
            )}
            {canDecideDeadline && (
              <Button variant="secondary" onClick={() => setExtendDeadlineOpen(true)}>
                Extend deadline
              </Button>
            )}
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

      {canHaveSubtasks && (
        <Card>
          <SubtasksPanel task={task} />
        </Card>
      )}

      {/* Only an individually-tracked task ever sets its own percentage directly — a
          TEAM/DEPARTMENT task's is always the rollup of its children (see SubtasksPanel/
          the implementation-task flow), so it gets no "Log progress" form or progress log
          at all. Every task, regardless of shape, still gets the Discussion panel below. */}
      {task.assigneeType === 'INDIVIDUAL' && <AddCommentForm taskId={task.id} currentPercentage={task.progressPercentage} />}

      <Card>
        <span className={styles.sectionHeading}>Discussion</span>
        <DiscussionPanel taskId={task.id} comments={task.comments} />
      </Card>

      {task.assigneeType === 'INDIVIDUAL' && (
        <Card>
          <span className={styles.sectionHeading}>Progress log</span>
          <CommentTimeline comments={task.comments} />
        </Card>
      )}

      <Card>
        <span className={styles.sectionHeading}>Reassignment history</span>
        <ReassignmentHistoryPanel reassignments={task.reassignments} />
      </Card>

      <Card>
        <span className={styles.sectionHeading}>Deadline extensions</span>
        <DeadlineExtensionHistoryPanel
          taskId={task.id}
          extensions={task.deadlineExtensions}
          canDecide={canDecideDeadline}
        />
      </Card>

      <ReassignTaskModal task={task} open={reassignOpen} onClose={() => setReassignOpen(false)} />
      <RequestExtensionModal task={task} open={requestExtensionOpen} onClose={() => setRequestExtensionOpen(false)} />
      <ExtendDeadlineModal task={task} open={extendDeadlineOpen} onClose={() => setExtendDeadlineOpen(false)} />

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
}
