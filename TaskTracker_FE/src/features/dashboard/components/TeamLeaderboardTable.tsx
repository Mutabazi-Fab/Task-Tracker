import { QueryBoundary } from '../../../components/feedback/QueryBoundary'
import { EmptyState } from '../../../components/ui/EmptyState'
import { useTeamLeaderboard } from '../hooks/useTeamLeaderboard'
import { TeamLeaderboardRow } from './TeamLeaderboardRow'
import styles from './TeamLeaderboardTable.module.css'

export function TeamLeaderboardTable() {
  const query = useTeamLeaderboard()

  return (
    <QueryBoundary query={query}>
      {(teams) =>
        teams.length === 0 ? (
          <EmptyState title="No teams yet" />
        ) : (
          <div className={styles.scrollWrap}>
            <div className={styles.table}>
              <div className={styles.header}>
                <span>Team</span>
                <span>Avg progress</span>
                <span className={styles.headerRight}>Tasks</span>
                <span className={styles.headerRight}>Done</span>
              </div>
              {teams.map((team) => (
                <TeamLeaderboardRow key={team.name} team={team} />
              ))}
            </div>
          </div>
        )
      }
    </QueryBoundary>
  )
}
