import { QueryBoundary } from '../../../components/feedback/QueryBoundary'
import { EmptyState } from '../../../components/ui/EmptyState'
import { usePeopleSummary } from '../hooks/usePeopleSummary'
import { PersonSummaryCard } from './PersonSummaryCard'
import styles from './PeopleSummaryGrid.module.css'

export function PeopleSummaryGrid() {
  const query = usePeopleSummary()

  return (
    <QueryBoundary query={query}>
      {(people) =>
        people.length === 0 ? (
          <EmptyState title="No people yet" />
        ) : (
          <div className={styles.grid}>
            {people.map((person) => (
              <PersonSummaryCard key={person.name} person={person} />
            ))}
          </div>
        )
      }
    </QueryBoundary>
  )
}
