import { useState } from 'react'
import { QueryBoundary } from '../../../components/feedback/QueryBoundary'
import { EmptyState } from '../../../components/ui/EmptyState'
import { Pagination } from '../../../components/ui/Pagination'
import { usePeopleSummary } from '../hooks/usePeopleSummary'
import { PersonSummaryCard } from './PersonSummaryCard'
import styles from './PeopleSummaryGrid.module.css'

// The grid is a fixed 4 columns on desktop (see PeopleSummaryGrid.module.css) so a page size
// of 16 is exactly 4 rows before pagination kicks in — narrower widths reflow to fewer
// columns, so "4 rows" only holds exactly at the 4-column width, but the bound stays 16
// either way.
const PAGE_SIZE = 16

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
