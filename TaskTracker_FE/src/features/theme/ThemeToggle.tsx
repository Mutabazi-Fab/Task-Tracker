import { SegmentedControl } from '../../components/ui/SegmentedControl'
import { useTheme } from './useTheme'
import type { Theme } from './ThemeProvider'

const OPTIONS: { label: string; value: Theme }[] = [
  { label: 'Field', value: 'field' },
  { label: 'Command', value: 'command' },
]

export function ThemeToggle() {
  const { theme, setTheme } = useTheme()
  return <SegmentedControl options={OPTIONS} value={theme} onChange={setTheme} aria-label="Theme" />
}
