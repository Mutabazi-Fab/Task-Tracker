import { Link } from 'react-router-dom'
import { ROUTES } from '../../../app/routes'
import { Avatar } from '../../../components/ui/Avatar'
import type { TeamMember } from '../../../types/team.types'
import styles from './TeamMemberChip.module.css'

interface TeamMemberChipProps {
  member: TeamMember
  /** Director-only — reassigns this team's leader to this member. Omitted entirely for a
   *  non-Director, and not shown for whoever is already the leader. */
  onMakeLeader?: () => void
  /** Director or this team's current leader — removes this member (reason captured by the
   *  caller before this fires). Omitted entirely if the viewer isn't allowed to manage this team. */
  onRemove?: () => void
}

/** One member, links to their profile. Shows a "Leader" tag for this team's leader —
 *  leadership is scoped per-team now, so this can only come from the membership row
 *  itself, never from the person. */
export function TeamMemberChip({ member, onMakeLeader, onRemove }: TeamMemberChipProps) {
  return (
    <div className={styles.chip}>
      <Link to={ROUTES.personProfile(member.personId)} className={styles.identity}>
        <Avatar name={member.fullName} size="sm" />
        <span className={styles.name}>{member.fullName}</span>
      </Link>
      {member.isLeader && <span className={styles.leaderTag}>Leader</span>}
      {!member.isLeader && onMakeLeader && (
        <button type="button" className={styles.action} onClick={onMakeLeader}>
          Make leader
        </button>
      )}
      {onRemove && (
        <button type="button" className={styles.actionDanger} onClick={onRemove}>
          Remove
        </button>
      )}
    </div>
  )
}
