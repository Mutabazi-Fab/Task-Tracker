import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ROUTES } from '../../app/routes'
import { PageHeader } from '../../components/layout/PageHeader'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { QueryBoundary } from '../../components/feedback/QueryBoundary'
import { EmptyState } from '../../components/ui/EmptyState'
import { useAuth } from '../auth/useAuth'
import { useTeams } from './hooks/useTeams'
import { CreateTeamModal } from './components/CreateTeamModal'
import styles from './TeamsListPage.module.css'

export function TeamsListPage() {
  const query = useTeams()
  const { isDirector } = useAuth()
  const [createOpen, setCreateOpen] = useState(false)

  return (
    <>
      <PageHeader
        breadcrumb="Throughline"
        title="Teams"
        right={isDirector ? <Button onClick={() => setCreateOpen(true)}>New team</Button> : undefined}
      />
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

      {isDirector && <CreateTeamModal open={createOpen} onClose={() => setCreateOpen(false)} />}
    </>
  )
}
