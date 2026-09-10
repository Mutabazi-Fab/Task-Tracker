import { useState } from 'react'
import { QueryBoundary } from '../../../components/feedback/QueryBoundary'
import { EmptyState } from '../../../components/ui/EmptyState'
import { Pagination } from '../../../components/ui/Pagination'
import { usePeopleSummary } from '../hooks/usePeopleSummary'
import { PersonSummaryCard } from './PersonSummaryCard'
import styles from './PeopleSummaryGrid.module.css'

// The grid itself is fluid (auto-fill, minmax(200px, 1fr) — see PeopleSummaryGrid.module.css),
// so its column count shifts with viewport width; there's no page size that guarantees a
// full last row at every width, and that's fine — a partial trailing row is normal grid
// behaviour. What actually matters here is not paginating away an org that already fits on
// one screen: 24 comfortably covers a small-to-mid organization with zero pages at all
// (Pagination renders nothing when totalPages <= 1), while still capping the DOM if
// headcount ever grows well past that.
const PAGE_SIZE = 24

/** The backend returns every person in one unpaginated list (bounded by org headcount,
 *  not transactional volume) — so pagination here is a client-side slice of an
 *  already-fetched array. */
export function PeopleSummaryGrid() {
  const query = usePeopleSummary()
  const [page, setPage] = useState(0)

  return (
    <QueryBoundary query={query}>
      {(people) => {
        const totalPages = Math.ceil(people.length / PAGE_SIZE)
        const pagePeople = people.slice(page * PAGE_SIZE, page * PAGE_SIZE + PAGE_SIZE)

        return people.length === 0 ? (
          <EmptyState title="No people yet" />
        ) : (
          <>
            <div className={styles.grid}>
              {pagePeople.map((person) => (
                <PersonSummaryCard key={person.name} person={person} />
              ))}
            </div>
            <div className={styles.pagination}>
              <Pagination page={page} totalPages={totalPages} onChange={setPage} />
            </div>
          </>
        )
      }}
    </QueryBoundary>
  )
}
