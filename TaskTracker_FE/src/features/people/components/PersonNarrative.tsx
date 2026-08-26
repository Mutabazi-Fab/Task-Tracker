import { formatPercentage } from '../../../lib/formatPercentage'
import type { PersonStatistics } from '../../../types/person.types'
import styles from './PersonNarrative.module.css'

function buildNarrative(stats: PersonStatistics): string {
  if (stats.tasksAssigned === 0) return 'No tasks assigned yet.'
  if (stats.fullyCompleted) return `Has completed all ${stats.tasksAssigned} assigned tasks.`

  const parts = [`${stats.tasksCompleted} of ${stats.tasksAssigned} assigned tasks complete`]
  parts.push(`${formatPercentage(stats.averageProgress)} average progress`)
  if (stats.tasksPending > 0) parts.push(`${stats.tasksPending} stalled at 0%`)
  return `${parts.join(', ')}.`
}

/** The one-line plain-language reading of a person's statistics. */
export function PersonNarrative({ stats }: { stats: PersonStatistics }) {
  return <p className={styles.narrative}>{buildNarrative(stats)}</p>
}
