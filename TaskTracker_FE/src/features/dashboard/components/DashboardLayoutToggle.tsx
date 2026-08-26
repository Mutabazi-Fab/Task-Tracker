import { SegmentedControl } from '../../../components/ui/SegmentedControl'

export type DashboardLayout = 'split' | 'trend-focus'

const OPTIONS: { label: string; value: DashboardLayout }[] = [
  { label: 'Split', value: 'split' },
  { label: 'Trend focus', value: 'trend-focus' },
]

interface DashboardLayoutToggleProps {
  value: DashboardLayout
  onChange: (value: DashboardLayout) => void
}

export function DashboardLayoutToggle({ value, onChange }: DashboardLayoutToggleProps) {
  return <SegmentedControl options={OPTIONS} value={value} onChange={onChange} aria-label="Dashboard layout" />
}
