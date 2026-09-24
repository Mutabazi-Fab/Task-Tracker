import { SegmentedControl } from '../../../components/ui/SegmentedControl'
import type { TaskSortValue } from '../../../types/task.types'

const OPTIONS: { label: string; value: TaskSortValue }[] = [
  { label: 'Recently updated', value: 'updatedAt,desc' },
  { label: 'Newest', value: 'createdAt,desc' },
  { label: 'All', value: 'none' },
]

interface TaskSortToggleProps {
  value: TaskSortValue
  onChange: (value: TaskSortValue) => void
}

/** "Recently updated" surfaces both a fresh task (its updatedAt starts equal to its createdAt) and an
 *  older one with fresh activity — the two things a manager actually wants near the top. */
export function TaskSortToggle({ value, onChange }: TaskSortToggleProps) {
  return <SegmentedControl options={OPTIONS} value={value} onChange={onChange} aria-label="Sort tasks" />
}
