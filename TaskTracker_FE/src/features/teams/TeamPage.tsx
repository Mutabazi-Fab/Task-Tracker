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

/**
 * A Director/Super Admin, or a member of THIS specific team, sees the full picture —
 * stats, roster, tasks, membership history. Anyone else (a Member looking at a team
 * they're not on) only sees the name and who leads it — the same "teams and who leads
 * them, nothing else" view the Teams list itself already gives everyone.
 */
export function TeamPage() {
  const { teamId } = useParams<{ teamId: string }>()
  const id = Number(teamId)
  const [addMemberOpen, setAddMemberOpen] = useState(false)

  const teamQuery = useTeam(id)
  const { isDirector, currentUser } = useAuth()

  const isMemberOfThisTeam = currentUser?.teams.some((t) => t.teamId === id) ?? false
  const isThisTeamsLeader = currentUser?.teams.some((t) => t.teamId === id && t.isLeader) ?? false
  const canManage = isDirector || isThisTeamsLeader
  const canViewFull = isDirector || isMemberOfThisTeam

  const statsQuery = useTeamStatistics(id, canViewFull)
  const membersQuery = useTeamMembers(id, canViewFull)

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

          {canViewFull ? (
            <>
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
            </>
          ) : (
            <p className={styles.restrictedNote}>You're not a member of this team.</p>
          )}

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
