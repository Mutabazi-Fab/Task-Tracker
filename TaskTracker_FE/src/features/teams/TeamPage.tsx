import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { PageHeader } from '../../components/layout/PageHeader'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { QueryBoundary } from '../../components/feedback/QueryBoundary'
import { useAuth } from '../auth/useAuth'
import { useDepartment } from '../departments/hooks/useDepartment'
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
  const { isDirector, isExecutive, currentUser } = useAuth()
  const navigate = useNavigate()
  // Mirrors the backend's TeamServiceImpl.isHeadOfDepartment exactly: Executive/Super Admin
  // may manage any team org-wide; a plain Director only the team of the department they
  // actually head, never merely one they belong to. Always called (never skipped) with a
  // NaN id when the team hasn't loaded yet — same pattern as SubtasksPanel's own
  // useDepartment call — so this never breaks the rules of hooks.
  const departmentQuery = useDepartment(teamQuery.data?.departmentId ?? NaN)

  const isMemberOfThisTeam = currentUser?.teams.some((t) => t.teamId === id) ?? false
  const isThisTeamsLeader = currentUser?.teams.some((t) => t.teamId === id && t.isLeader) ?? false
  const headsThisTeamsDepartment = isDirector && departmentQuery.data?.headDirectorId === currentUser?.id
  // Add/remove: this team's own Leader too (mirrors requireDirectorOfTeamsDepartmentOrTeamLeader).
  const canManage = isExecutive || headsThisTeamsDepartment || isThisTeamsLeader
  // Reassigning who leads the team is narrower — never the current leader themselves,
  // only whoever actually has authority OVER the team (mirrors setTeamLeader's own check,
  // which has no Team-Leader-self-service path).
  const canReassignLeader = isExecutive || headsThisTeamsDepartment
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
            onBack={() => navigate(-1)}
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
                <TeamMemberChips
                  teamId={id}
                  teamName={team.name}
                  canManage={canManage}
                  canReassignLeader={canReassignLeader}
                />
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
