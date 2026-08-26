import { useParams } from 'react-router-dom'
import { PageHeader } from '../../components/layout/PageHeader'
import { Card } from '../../components/ui/Card'
import { QueryBoundary } from '../../components/feedback/QueryBoundary'
import { useTeam } from './hooks/useTeam'
import { useTeamStatistics } from './hooks/useTeamStatistics'
import { TeamHeader } from './components/TeamHeader'
import { TeamStatsRow } from './components/TeamStatsRow'
import { TeamMemberChips } from './components/TeamMemberChips'
import { TeamTaskList } from './components/TeamTaskList'
import styles from './TeamPage.module.css'

export function TeamPage() {
  const { teamId } = useParams<{ teamId: string }>()
  const id = Number(teamId)

  const teamQuery = useTeam(id)
  const statsQuery = useTeamStatistics(id)

  return (
    <QueryBoundary query={teamQuery}>
      {(team) => (
        <>
          <PageHeader breadcrumb="Throughline / Teams" title={team.name} />

          <Card>
            <TeamHeader team={team} />
          </Card>

          <QueryBoundary query={statsQuery}>{(stats) => <TeamStatsRow stats={stats} />}</QueryBoundary>

          <Card>
            <span className={styles.sectionHeading}>Members</span>
            <TeamMemberChips teamId={id} />
          </Card>

          <Card>
            <span className={styles.sectionHeading}>Tasks</span>
            <TeamTaskList teamId={id} />
          </Card>
        </>
      )}
    </QueryBoundary>
  )
}
