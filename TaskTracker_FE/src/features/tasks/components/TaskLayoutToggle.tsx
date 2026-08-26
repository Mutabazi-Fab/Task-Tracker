import { SegmentedControl } from '../../../components/ui/SegmentedControl'

export type TaskLayout = 'table' | 'lanes'

const OPTIONS: { label: string; value: TaskLayout }[] = [
  { label: 'Table', value: 'table' },
  { label: 'Lanes', value: 'lanes' },
]

interface TaskLayoutToggleProps {
  value: TaskLayout
  onChange: (value: TaskLayout) => void
}

export function TaskLayoutToggle({ value, onChange }: TaskLayoutToggleProps) {
  return <SegmentedControl options={OPTIONS} value={value} onChange={onChange} aria-label="Task layout" />
}
