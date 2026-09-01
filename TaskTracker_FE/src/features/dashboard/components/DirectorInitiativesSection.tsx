import { Card } from '../../../components/ui/Card'
import { EmptyState } from '../../../components/ui/EmptyState'
import { QueryBoundary } from '../../../components/feedback/QueryBoundary'
import { TaskCard } from '../../tasks/components/TaskCard'
import { useAuth } from '../../auth/useAuth'
import { useDirectorTasks } from '../hooks/useDirectorTasks'
import styles from './DirectorInitiativesSection.module.css'

/**
 * Only rendered for a Director/Super Admin (see DashboardPage) — the Director's Dashboard
 * default view from the spec: only the top-level tasks THIS Director created, each
 * already showing its auto-calculated % (rolled up from subtasks) via TaskCard.
 */
export function DirectorInitiativesSection() {
  const { currentUser } = useAuth()
  const query = useDirectorTasks(currentUser?.id ?? NaN)

  return (
    <Card>
      <div className={styles.sectionHeading}>My initiatives</div>
      <QueryBoundary query={query}>
        {(result) =>
          result.content.length === 0 ? (
            <EmptyState title="No initiatives yet" description="Top-level tasks you create show up here." />
          ) : (
            <div className={styles.grid}>
              {result.content.map((task) => (
                <TaskCard key={task.id} task={task} />
              ))}
            </div>
          )
        }
      </QueryBoundary>
    </Card>
  )
}
