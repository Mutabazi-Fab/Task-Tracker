import { QueryBoundary } from '../../../components/feedback/QueryBoundary'
import { EmptyState } from '../../../components/ui/EmptyState'
import { useTeamMembers } from '../hooks/useTeamMembers'
import { TeamMemberChip } from './TeamMemberChip'
import styles from './TeamMemberChips.module.css'

export function TeamMemberChips({ teamId }: { teamId: number }) {
  const query = useTeamMembers(teamId)

  return (
    <QueryBoundary query={query}>
      {(members) =>
        members.length === 0 ? (
          <EmptyState title="No members yet" />
        ) : (
          <div className={styles.wrap}>
            {members.map((person) => (
              <TeamMemberChip key={person.id} person={person} />
            ))}
          </div>
        )
      }
    </QueryBoundary>
  )
}
