import { EmptyState } from '../../../components/ui/EmptyState'
import type { PersonTaskHistoryItem } from '../../../types/person.types'
import { PersonTaskHistoryRow } from './PersonTaskHistoryRow'

export function PersonTaskHistoryTable({ history }: { history: PersonTaskHistoryItem[] }) {
  if (history.length === 0) {
    return <EmptyState title="No task history" description="Nothing this person is connected to yet." />
  }

  return (
    <div>
      {history.map((item) => (
        <PersonTaskHistoryRow key={item.taskId} item={item} />
      ))}
    </div>
  )
}
