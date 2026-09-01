import { Card } from '../../../components/ui/Card'
import { QueryBoundary } from '../../../components/feedback/QueryBoundary'
import { useAuth } from '../../auth/useAuth'
import { usePersonStatistics } from '../../people/hooks/usePersonStatistics'
import { PersonStatsRow } from '../../people/components/PersonStatsRow'
import { PersonNarrative } from '../../people/components/PersonNarrative'
import { useTasks } from '../../tasks/hooks/useTasks'
import { TaskTable } from '../../tasks/components/TaskTable'
import styles from './MyDashboardSummary.module.css'

const MY_TASKS_SIZE = 20

/**
 * The whole dashboard for anyone who isn't a Director/Super Admin — not an addition
 * alongside the org-wide charts/leaderboard/people-summary, a replacement for them. A
 * Member sees a summary of what's assigned to them and how it's going, and nothing about
 * the rest of the org (see DashboardPage, which renders this instead of everything else).
 */
export function MyDashboardSummary() {
  const { currentUser } = useAuth()
  const personId = currentUser?.id ?? NaN
  const statsQuery = usePersonStatistics(personId)
  const tasksQuery = useTasks({ assignedPersonId: personId, page: 0, size: MY_TASKS_SIZE })

  return (
    <>
      <QueryBoundary query={statsQuery}>
        {(stats) => (
          <>
            <PersonStatsRow stats={stats} />
            <PersonNarrative stats={stats} />
          </>
        )}
      </QueryBoundary>

      <Card>
        <div className={styles.sectionHeading}>My tasks</div>
        <QueryBoundary query={tasksQuery}>{(result) => <TaskTable tasks={result.content} />}</QueryBoundary>
      </Card>
    </>
  )
}
