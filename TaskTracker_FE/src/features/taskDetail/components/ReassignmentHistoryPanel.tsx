import { EmptyState } from '../../../components/ui/EmptyState'
import type { TaskReassignment } from '../../../types/reassignment.types'
import { ReassignmentHistoryItem } from './ReassignmentHistoryItem'

/** The ownership audit trail — oldest first, as the backend returns it. */
export function ReassignmentHistoryPanel({ reassignments }: { reassignments: TaskReassignment[] }) {
  if (reassignments.length === 0) {
    return <EmptyState title="Never reassigned" description="This task has had one owner since it was created." />
  }

  return (
    <div>
      {reassignments.map((reassignment) => (
        <ReassignmentHistoryItem key={reassignment.id} reassignment={reassignment} />
      ))}
    </div>
  )
}
