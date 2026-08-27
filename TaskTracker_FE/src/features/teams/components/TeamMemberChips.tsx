import { useState } from 'react'
import { QueryBoundary } from '../../../components/feedback/QueryBoundary'
import { EmptyState } from '../../../components/ui/EmptyState'
import { useAuth } from '../../auth/useAuth'
import { useTeamMembers } from '../hooks/useTeamMembers'
import { useSetTeamLeader } from '../hooks/useSetTeamLeader'
import { RemoveMemberModal } from './RemoveMemberModal'
import { TeamMemberChip } from './TeamMemberChip'
import type { TeamMember } from '../../../types/team.types'
import styles from './TeamMemberChips.module.css'

interface TeamMemberChipsProps {
  teamId: number
  /** Director, or the current leader of THIS team — the only two roles allowed to
   *  add/remove members (checked server-side too; this only controls whether the
   *  buttons show up at all). */
  canManage: boolean
}

export function TeamMemberChips({ teamId, canManage }: TeamMemberChipsProps) {
  const { isDirector, currentUser } = useAuth()
  const query = useTeamMembers(teamId)
  const setLeader = useSetTeamLeader(teamId)
  const [removing, setRemoving] = useState<TeamMember | null>(null)

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
                  onMakeLeader={
                    isDirector && currentUser
                      ? () => setLeader.mutate({ personId: member.personId, changedById: currentUser.id })
                      : undefined
                  }
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
          </>
        )
      }
    </QueryBoundary>
  )
}
