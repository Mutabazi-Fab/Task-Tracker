import type { TaskStatus } from '../types/task.types'

/**
 * Maps a TaskStatus to the CSS module class key each status-aware component
 * (StatusChip, ProgressBar, the dashboard donut) uses to pick its
 * var(--status-*) pair. This is the only place that mapping is written down.
 */
export function statusColorKey(status: TaskStatus): 'completed' | 'ongoing' | 'pending' {
  switch (status) {
    case 'COMPLETED':
      return 'completed'
    case 'ONGOING':
      return 'ongoing'
    case 'PENDING':
      return 'pending'
  }
}
