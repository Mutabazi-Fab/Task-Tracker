import { SegmentedControl } from '../../../components/ui/SegmentedControl'
import type { TaskStatus } from '../../../types/task.types'

export type TaskStatusFilterValue = TaskStatus | 'ALL'

const OPTIONS: { label: string; value: TaskStatusFilterValue }[] = [
  { label: 'All', value: 'ALL' },
  { label: 'Pending', value: 'PENDING' },
  { label: 'Ongoing', value: 'ONGOING' },
  { label: 'Completed', value: 'COMPLETED' },
]

interface TaskStatusFilterProps {
  value: TaskStatusFilterValue
  onChange: (value: TaskStatusFilterValue) => void
}

export function TaskStatusFilter({ value, onChange }: TaskStatusFilterProps) {
  return <SegmentedControl options={OPTIONS} value={value} onChange={onChange} aria-label="Filter by status" />
}
