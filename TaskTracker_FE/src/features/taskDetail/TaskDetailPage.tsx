import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { PageHeader } from '../../components/layout/PageHeader'
import { Card } from '../../components/ui/Card'
import { Button } from '../../components/ui/Button'
import { QueryBoundary } from '../../components/feedback/QueryBoundary'
import { useTaskDetail } from './hooks/useTaskDetail'
import { TaskDetailHeader } from './components/TaskDetailHeader'
import { TaskProgressPanel } from './components/TaskProgressPanel'
import { TaskProgressSparkline } from './components/TaskProgressSparkline'
import { AssignmentMetaPanel } from './components/AssignmentMetaPanel'
import { AddCommentForm } from './components/AddCommentForm'
import { CommentTimeline } from './components/CommentTimeline'
import { ReassignmentHistoryPanel } from './components/ReassignmentHistoryPanel'
import { ReassignTaskModal } from './components/ReassignTaskModal'
import styles from './TaskDetailPage.module.css'

/** Composes the sections below. No data-fetching or layout logic of its own. */
export function TaskDetailPage() {
  const { taskId } = useParams<{ taskId: string }>()
  const query = useTaskDetail(Number(taskId))
  const [reassignOpen, setReassignOpen] = useState(false)

  return (
    <QueryBoundary query={query}>
      {(task) => (
        <>
          <PageHeader
            breadcrumb="Throughline / Tasks"
            title={task.taskCode}
            right={<Button variant="secondary" onClick={() => setReassignOpen(true)}>Reassign</Button>}
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
        </>
      )}
    </QueryBoundary>
  )
}
