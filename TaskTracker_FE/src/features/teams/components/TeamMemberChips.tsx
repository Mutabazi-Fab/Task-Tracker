import { useState } from 'react'
import { QueryBoundary } from '../../../components/feedback/QueryBoundary'
import { EmptyState } from '../../../components/ui/EmptyState'
import { useTeamMembers } from '../hooks/useTeamMembers'
import { MakeLeaderModal } from './MakeLeaderModal'
import { RemoveMemberModal } from './RemoveMemberModal'
import { TeamMemberChip } from './TeamMemberChip'
import type { TeamMember } from '../../../types/team.types'
import styles from './TeamMemberChips.module.css'

interface TeamMemberChipsProps {
  teamId: number
  teamName: string
  /** Add/remove authority: Executive/Super Admin, a Director who heads THIS team's own
   *  department, or the current leader. Checked server-side too; this only controls the buttons. */
  canManage: boolean
  /** Narrower than canManage — never the current leader, only Executive/Super Admin or a
   *  Director who heads this department (mirrors TeamServiceImpl.setTeamLeader). */
  canReassignLeader: boolean
}

export function TeamMemberChips({ teamId, teamName, canManage, canReassignLeader }: TeamMemberChipsProps) {
  const query = useTeamMembers(teamId)
  const [removing, setRemoving] = useState<TeamMember | null>(null)
  // Confirmed with a yes/no modal, not fired straight from the click — changing who leads
  // a team isn't something to undo by accident, and the confirm text names exactly who's
  // about to become leader, not just a bare "Make leader" button with no second thought.
  const [makingLeader, setMakingLeader] = useState<TeamMember | null>(null)

  return (
    <QueryBoundary query={query}>
      {(members) =>
        members.length === 0 ? (
          <EmptyState title="No members yet" />
        ) : (
          <>
            <div className={styles.wrap}>
              {members.map((member) => (
                <TeamMemberChip
                  key={member.personId}
                  member={member}
                  onMakeLeader={canReassignLeader ? () => setMakingLeader(member) : undefined}
                  onRemove={canManage ? () => setRemoving(member) : undefined}
                />
              ))}
            </div>

            {removing && (
              <RemoveMemberModal
                teamId={teamId}
                personId={removing.personId}
                personName={removing.fullName}
                open
                onClose={() => setRemoving(null)}
              />
            )}

            {makingLeader && (
              <MakeLeaderModal
                teamId={teamId}
                teamName={teamName}
                personId={makingLeader.personId}
                personName={makingLeader.fullName}
                open
                onClose={() => setMakingLeader(null)}
              />
            )}
          </>
        )
      }
    </QueryBoundary>
  )
}
