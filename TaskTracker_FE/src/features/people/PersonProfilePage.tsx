import { useParams } from 'react-router-dom'
import { PageHeader } from '../../components/layout/PageHeader'
import { Card } from '../../components/ui/Card'
import { QueryBoundary } from '../../components/feedback/QueryBoundary'
import { useAuth } from '../auth/useAuth'
import { usePerson } from './hooks/usePerson'
import { usePersonStatistics } from './hooks/usePersonStatistics'
import { usePersonTaskHistory } from './hooks/usePersonTaskHistory'
import { PersonProfileHeader } from './components/PersonProfileHeader'
import { PersonStatsRow } from './components/PersonStatsRow'
import { PersonNarrative } from './components/PersonNarrative'
import { PersonTaskHistoryTable } from './components/PersonTaskHistoryTable'
import { PersonAdminControls } from './components/PersonAdminControls'
import styles from './PersonProfilePage.module.css'

export function PersonProfilePage() {
  const { personId } = useParams<{ personId: string }>()
  const id = Number(personId)
  const { isSuperAdmin } = useAuth()

  const personQuery = usePerson(id)
  const statsQuery = usePersonStatistics(id)
  const historyQuery = usePersonTaskHistory(id)

  return (
    <QueryBoundary query={personQuery}>
      {(person) => (
        <>
          <PageHeader breadcrumb="Throughline / People" title={person.fullName} />

          <Card>
            <PersonProfileHeader person={person} />
          </Card>

          {isSuperAdmin && <PersonAdminControls person={person} />}

          <QueryBoundary query={statsQuery}>
            {(stats) => (
              <>
                <PersonStatsRow stats={stats} />
                <PersonNarrative stats={stats} />
              </>
            )}
          </QueryBoundary>

          <Card>
            <span className={styles.sectionHeading}>Task history</span>
            <QueryBoundary query={historyQuery}>{(history) => <PersonTaskHistoryTable history={history} />}</QueryBoundary>
          </Card>
        </>
      )}
    </QueryBoundary>
  )
}
