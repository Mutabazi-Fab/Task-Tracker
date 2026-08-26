import { Link } from 'react-router-dom'
import { ROUTES } from '../../app/routes'
import { PageHeader } from '../../components/layout/PageHeader'
import { Card } from '../../components/ui/Card'
import { QueryBoundary } from '../../components/feedback/QueryBoundary'
import { EmptyState } from '../../components/ui/EmptyState'
import { useTeams } from './hooks/useTeams'
import styles from './TeamsListPage.module.css'

export function TeamsListPage() {
  const query = useTeams()

  return (
    <>
      <PageHeader breadcrumb="Throughline" title="Teams" />
      <QueryBoundary query={query}>
        {(teams) =>
          teams.length === 0 ? (
            <EmptyState title="No teams yet" />
          ) : (
            <div className={styles.grid}>
              {teams.map((team) => (
                <Link key={team.id} to={ROUTES.team(team.id)} className={styles.link}>
                  <Card padding="sm">
                    <span className={styles.name}>{team.name}</span>
                    <span className={styles.leader}>{team.leaderName ? `Led by ${team.leaderName}` : 'No leader'}</span>
                    <span className={styles.meta}>{team.memberCount} members</span>
                  </Card>
                </Link>
              ))}
            </div>
          )
        }
      </QueryBoundary>
    </>
  )
}
