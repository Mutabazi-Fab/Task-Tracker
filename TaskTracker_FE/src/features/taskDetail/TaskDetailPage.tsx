import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { PageHeader } from '../../components/layout/PageHeader'
import { Card } from '../../components/ui/Card'
import { Button } from '../../components/ui/Button'
import { EmptyState } from '../../components/ui/EmptyState'
import { NewBadge } from '../../components/ui/NewBadge'
import { QueryBoundary } from '../../components/feedback/QueryBoundary'
import { ROUTES } from '../../app/routes'
import { isRecentlyCreated } from '../../lib/isRecentlyCreated'
import { useAuth } from '../auth/useAuth'
import { useDepartment } from '../departments/hooks/useDepartment'
import { useTaskDetail } from './hooks/useTaskDetail'
import { useSetPinned } from './hooks/useSetPinned'
import { TaskDetailHeader } from './components/TaskDetailHeader'
import { TaskProgressPanel } from './components/TaskProgressPanel'
import { TaskProgressSparkline } from './components/TaskProgressSparkline'
import { AssignmentMetaPanel } from './components/AssignmentMetaPanel'
import { AddCommentForm } from './components/AddCommentForm'
import { useCanLogProgress } from './hooks/useCanLogProgress'
import { AccessGrantsPanel } from '../accessGrants/components/AccessGrantsPanel'
import { GrantedAccessBanner } from '../accessGrants/components/GrantedAccessBanner'
import { CommentTimeline } from './components/CommentTimeline'
import { DiscussionPanel } from './components/DiscussionPanel'
import { ReassignmentHistoryPanel } from './components/ReassignmentHistoryPanel'
import { ReassignTaskModal } from './components/ReassignTaskModal'
import { RequestExtensionModal } from './components/RequestExtensionModal'
import { ExtendDeadlineModal } from './components/ExtendDeadlineModal'
import { DeadlineExtensionHistoryPanel } from './components/DeadlineExtensionHistoryPanel'
import { SubtasksPanel } from './components/SubtasksPanel'
import { DocumentsPanel } from './components/DocumentsPanel'
import { DeleteTaskModal } from './components/DeleteTaskModal'
import type { TaskDetail } from '../../types/task.types'
import styles from './TaskDetailPage.module.css'

/** Thin wrapper — waits for the task, then hands it to TaskDetailBody. */
export function TaskDetailPage() {
  const { taskId } = useParams<{ taskId: string }>()
  const query = useTaskDetail(Number(taskId))
  const navigate = useNavigate()

  if (query.isError && query.error.status === 404) {
    return (
      <>
        <PageHeader breadcrumb="Throughline / Tasks" title="Task deleted" onBack={() => navigate(-1)} />
        <Card>
          <EmptyState
            title="This task has been deleted"
            description="It no longer exists — move on to other tasks."
            action={
              <Link to={ROUTES.tasks}>
                <Button variant="secondary">View all tasks</Button>
              </Link>
            }
          />
        </Card>
      </>
    )
  }

  if (query.isError && query.error.status === 403) {
    return (
      <>
        <PageHeader breadcrumb="Throughline / Tasks" title="No access" onBack={() => navigate(-1)} />
        <Card>
          <EmptyState
            title="This task isn't shared with you"
            description="It belongs to another department. Ask a Super Admin or the CEO to share it with you."
            action={
              <Link to={ROUTES.tasks}>
                <Button variant="secondary">Back to tasks</Button>
              </Link>
            }
          />
        </Card>
      </>
    )
  }

  return <QueryBoundary query={query}>{(task) => <TaskDetailBody task={task} />}</QueryBoundary>
}

function TaskDetailBody({ task }: { task: TaskDetail }) {
  const { isDirector, isExecutive, currentUser } = useAuth()
  const canLogProgress = useCanLogProgress(task)
  const navigate = useNavigate()
  const [reassignOpen, setReassignOpen] = useState(false)
  const [deleteOpen, setDeleteOpen] = useState(false)
  const [requestExtensionOpen, setRequestExtensionOpen] = useState(false)
  const [extendDeadlineOpen, setExtendDeadlineOpen] = useState(false)

  const isDepartmentAssigned = task.assigneeType === 'DEPARTMENT'
  const departmentQuery = useDepartment(isDepartmentAssigned ? (task.assigneeId ?? NaN) : NaN)
  const setPinned = useSetPinned(task.id)

  // A TEAM-assigned task (top-level or depth-1 implementation task) not yet at max depth,
  // or a DEPARTMENT task (creating its one implementation task) — an INDIVIDUAL task is
  // always a dead end, no Subtasks panel.
  const canHaveSubtasks = (task.assigneeType === 'TEAM' && task.depth < 2) || isDepartmentAssigned
  // Same simplifying comparison CreateTeamForm relies on: a Director's own department
  // membership already matches their headship. Executive/Super Admin bypass this everywhere below.
  const headsTasksDepartment = task.taskDepartmentId !== null && task.taskDepartmentId === currentUser?.departmentId
  // Mirrors TaskServiceImpl.requireCanDelete: Executive/Super Admin always; a plain Director only their
  // own created task within the department they head.
  const canDelete =
    isExecutive || (isDirector && task.assignedById === currentUser?.id && headsTasksDepartment)
  // Mirrors TaskServiceImpl.setPinned: a Director who heads this task's department, or Executive/Super Admin.
  const canPin = isExecutive || (isDirector && headsTasksDepartment)
  // Mirrors TaskServiceImpl.reassignTask: moving a Department task to another department
  // is Executive/Super-Admin-only; otherwise a Director who heads this department, an
  // Executive/Super Admin, or this task's own Team Leader.
  const canReassign = isDepartmentAssigned
    ? isExecutive
    : isExecutive ||
      (isDirector && headsTasksDepartment) ||
      (task.owningTeamId !== null && (currentUser?.teams.some((t) => t.teamId === task.owningTeamId && t.isLeader) ?? false))

  // Mirrors TaskServiceImpl.isDeadlineOverrideTier: Executive-or-above for a Department
  // task, Director-or-above otherwise.
  const isDeadlineOverrideTier = isDepartmentAssigned ? isExecutive : isDirector
  // Mirrors TaskServiceImpl.resolveAccountablePerson: this task's Team Leader, individual
  // assignee, or (a Department task) its head Director.
  const isAccountablePerson = isDepartmentAssigned
    ? departmentQuery.data?.headDirectorId === currentUser?.id
    : task.assigneeType === 'TEAM'
      ? (currentUser?.teams.some((t) => t.teamId === task.assigneeId && t.isLeader) ?? false)
      : task.assigneeId === currentUser?.id
  // The chain-of-command deadline decider (NOT necessarily assignedById — see
  // resolveDeadlineDecider) or the override tier.
  const canDecideDeadline = isDeadlineOverrideTier || task.deadlineDeciderId === currentUser?.id
  // The accountable person or override tier, but never the same person who'd also decide
  // it — that's what "Extend deadline" is for instead.
  const canRequestExtension = (isDeadlineOverrideTier || isAccountablePerson) && !canDecideDeadline

  return (
    <>
      <PageHeader
        breadcrumb="Throughline / Tasks"
        title={task.taskCode}
        onBack={() => navigate(-1)}
        titleBadge={isRecentlyCreated(task.createdAt) ? <NewBadge /> : undefined}
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

      <GrantedAccessBanner resourceType="TASK" resourceId={task.id} />

      {/* Lets a viewer climb back up the hierarchy — parentTaskId isn't otherwise surfaced anywhere on this page. */}
      {task.parentTaskId !== null && (
        <Link to={ROUTES.taskDetail(task.parentTaskId)} className={styles.parentLink}>
          ← Part of {task.parentTaskTitle ?? task.parentTaskCode}
        </Link>
      )}

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

      {/* Only an individually-tracked task sets its own percentage directly — a TEAM/DEPARTMENT task's is
         always a rollup, so it gets no "Log progress" form. */}
      {canLogProgress && <AddCommentForm taskId={task.id} currentPercentage={task.progressPercentage} />}

      {/* Documents rides alongside Discussion as a narrow sidebar — usually just a handful of files. */}
      <div className={styles.discussionRow}>
        <Card>
          <span className={styles.sectionHeading}>Discussion</span>
          <DiscussionPanel taskId={task.id} comments={task.comments} />
        </Card>
        <Card>
          <DocumentsPanel taskId={task.id} documents={task.documents} />
        </Card>
      </div>

      <AccessGrantsPanel resourceType="TASK" resourceId={task.id} />

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
