import { useParams } from 'react-router-dom'
import { PageHeader } from '../../components/layout/PageHeader'
import { Card } from '../../components/ui/Card'
import { QueryBoundary } from '../../components/feedback/QueryBoundary'
import { usePerson } from './hooks/usePerson'
import { usePersonStatistics } from './hooks/usePersonStatistics'
import { usePersonTaskHistory } from './hooks/usePersonTaskHistory'
import { PersonProfileHeader } from './components/PersonProfileHeader'
import { PersonStatsRow } from './components/PersonStatsRow'
import { PersonNarrative } from './components/PersonNarrative'
import { PersonTaskHistoryTable } from './components/PersonTaskHistoryTable'
import styles from './PersonProfilePage.module.css'

export function PersonProfilePage() {
  const { personId } = useParams<{ personId: string }>()
  const id = Number(personId)

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
