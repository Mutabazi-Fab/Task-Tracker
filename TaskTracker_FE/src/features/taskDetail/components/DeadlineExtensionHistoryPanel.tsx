import { EmptyState } from '../../../components/ui/EmptyState'
import type { DeadlineExtension } from '../../../types/deadlineExtension.types'
import { DeadlineExtensionHistoryItem } from './DeadlineExtensionHistoryItem'

interface DeadlineExtensionHistoryPanelProps {
  taskId: number
  extensions: DeadlineExtension[]
  canDecide: boolean
}

/** The deadline audit trail — oldest first, as the backend returns it (mirrors
 *  ReassignmentHistoryPanel). */
export function DeadlineExtensionHistoryPanel({ taskId, extensions, canDecide }: DeadlineExtensionHistoryPanelProps) {
  if (extensions.length === 0) {
    return <EmptyState title="No extension requests" description="This task's deadline has never been changed." />
  }

  return (
    <div>
      {extensions.map((extension) => (
        <DeadlineExtensionHistoryItem key={extension.id} extension={extension} taskId={taskId} canDecide={canDecide} />
      ))}
    </div>
  )
}
