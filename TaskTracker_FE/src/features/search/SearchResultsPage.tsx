import { useSearchParams } from 'react-router-dom'
import { PageHeader } from '../../components/layout/PageHeader'
import { EmptyState } from '../../components/ui/EmptyState'
import { QueryBoundary } from '../../components/feedback/QueryBoundary'
import { useGlobalSearch } from './hooks/useGlobalSearch'
import { SearchResultsSummary } from './components/SearchResultsSummary'
import { PeopleResultsSection } from './components/PeopleResultsSection'
import { TaskResultsSection } from './components/TaskResultsSection'
import styles from './SearchResultsPage.module.css'

export function SearchResultsPage() {
  const [params] = useSearchParams()
  const q = params.get('q')?.trim() ?? ''
  const { searchQuery: query } = useGlobalSearch(q)

  return (
    <>
      <PageHeader breadcrumb="Throughline" title="Search" />

      {q.length === 0 ? (
        <EmptyState title="Nothing to search yet" description="Type into the search box in the header." />
      ) : (
        <QueryBoundary query={query}>
          {(result) => (
            <>
              <SearchResultsSummary query={q} peopleCount={result.people.length} taskCount={result.tasks.length} />

              <div className={styles.section}>
                <span className={styles.sectionHeading}>People</span>
                <PeopleResultsSection people={result.people} />
              </div>

              <div className={styles.section}>
                <span className={styles.sectionHeading}>Tasks</span>
                <TaskResultsSection tasks={result.tasks} />
              </div>
            </>
          )}
        </QueryBoundary>
      )}
    </>
  )
}
