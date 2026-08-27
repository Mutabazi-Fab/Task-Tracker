import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { PageHeader } from '../../components/layout/PageHeader'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { QueryBoundary } from '../../components/feedback/QueryBoundary'
import { useAuth } from '../auth/useAuth'
import { useTeam } from './hooks/useTeam'
import { useTeamStatistics } from './hooks/useTeamStatistics'
import { useTeamMembers } from './hooks/useTeamMembers'
import { TeamHeader } from './components/TeamHeader'
import { TeamStatsRow } from './components/TeamStatsRow'
import { TeamMemberChips } from './components/TeamMemberChips'
import { TeamTaskList } from './components/TeamTaskList'
import { AddMemberModal } from './components/AddMemberModal'
import { MembershipHistoryPanel } from './components/MembershipHistoryPanel'
import styles from './TeamPage.module.css'

export function TeamPage() {
  const { teamId } = useParams<{ teamId: string }>()
  const id = Number(teamId)
  const [addMemberOpen, setAddMemberOpen] = useState(false)

  const teamQuery = useTeam(id)
  const statsQuery = useTeamStatistics(id)
  const membersQuery = useTeamMembers(id)
  const { isDirector, currentUser } = useAuth()

  // Director, or the current leader of THIS specific team — leadership is scoped per-team.
  const isThisTeamsLeader = currentUser?.teams.some((t) => t.teamId === id && t.isLeader) ?? false
  const canManage = isDirector || isThisTeamsLeader

  return (
    <QueryBoundary query={teamQuery}>
      {(team) => (
        <>
          <PageHeader
            breadcrumb="Throughline / Teams"
            title={team.name}
            right={canManage ? <Button onClick={() => setAddMemberOpen(true)}>Add member</Button> : undefined}
          />

          <Card>
            <TeamHeader team={team} />
          </Card>

          <QueryBoundary query={statsQuery}>{(stats) => <TeamStatsRow stats={stats} />}</QueryBoundary>

          <Card>
            <span className={styles.sectionHeading}>Members</span>
            <TeamMemberChips teamId={id} canManage={canManage} />
          </Card>

          <Card>
            <span className={styles.sectionHeading}>Tasks</span>
            <TeamTaskList teamId={id} />
          </Card>

          <Card>
            <span className={styles.sectionHeading}>Membership history</span>
            <MembershipHistoryPanel teamId={id} />
          </Card>

          {canManage && (
            <AddMemberModal
              teamId={id}
              existingMembers={membersQuery.data ?? []}
              open={addMemberOpen}
              onClose={() => setAddMemberOpen(false)}
            />
          )}
        </>
      )}
    </QueryBoundary>
  )
}
