import { useState } from 'react'
import { QueryBoundary } from '../../../components/feedback/QueryBoundary'
import { EmptyState } from '../../../components/ui/EmptyState'
import { Pagination } from '../../../components/ui/Pagination'
import { useTeamLeaderboard } from '../hooks/useTeamLeaderboard'
import { TeamLeaderboardRow } from './TeamLeaderboardRow'
import styles from './TeamLeaderboardTable.module.css'

const PAGE_SIZE = 8

/** The backend returns every team in one unpaginated list (bounded by the org's team
 *  count, not transactional volume) — so pagination here is a client-side slice of an
 *  already-fetched array rather than a real paged query, at 8 rows per page. */
export function TeamLeaderboardTable() {
  const query = useTeamLeaderboard()
  const [page, setPage] = useState(0)

  return (
    <QueryBoundary query={query}>
      {(teams) => {
        const totalPages = Math.ceil(teams.length / PAGE_SIZE)
        const pageTeams = teams.slice(page * PAGE_SIZE, page * PAGE_SIZE + PAGE_SIZE)

        return teams.length === 0 ? (
          <EmptyState title="No teams yet" />
        ) : (
          <>
            <div className={styles.scrollWrap}>
              <div className={styles.table}>
                <div className={styles.header}>
                  <span>Team</span>
                  <span>Avg progress</span>
                  <span className={styles.headerRight}>Tasks</span>
                  <span className={styles.headerRight}>Done</span>
                </div>
                {pageTeams.map((team) => (
                  <TeamLeaderboardRow key={team.name} team={team} />
                ))}
              </div>
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
