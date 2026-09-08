import { QueryBoundary } from '../../../components/feedback/QueryBoundary'
import { EmptyState } from '../../../components/ui/EmptyState'
import { formatDateTime } from '../../../lib/formatDate'
import { useMembershipHistory } from '../hooks/useMembershipHistory'
import type { TeamMembershipChangeAction } from '../../../types/team.types'
import styles from './MembershipHistoryPanel.module.css'

const ACTION_LABEL: Record<TeamMembershipChangeAction, string> = {
  ADDED: 'Added',
  REMOVED: 'Removed',
  LEADER_CHANGED: 'Made leader',
}

const ACTION_CLASS: Record<TeamMembershipChangeAction, string> = {
  ADDED: styles.actionAdded,
  REMOVED: styles.actionRemoved,
  LEADER_CHANGED: styles.actionLeader,
}

/** Append-only audit log of every add/remove/leader-change for this team, newest first —
 *  never edited, survives the current roster moving on. */
export function MembershipHistoryPanel({ teamId }: { teamId: number }) {
  const query = useMembershipHistory(teamId)

  return (
    <QueryBoundary query={query}>
      {(history) =>
        history.length === 0 ? (
          <EmptyState title="No membership changes yet" />
        ) : (
          <div className={styles.list}>
            {history.map((change) => (
              <div key={change.id} className={styles.row}>
                <span className={styles.line}>
                  <span className={ACTION_CLASS[change.action]}>{ACTION_LABEL[change.action]}</span> {change.personName}{' '}
                  — {change.reason}
                </span>
                <span className={styles.meta}>
                  by {change.changedByName} · {formatDateTime(change.timestamp)}
                </span>
              </div>
            ))}
          </div>
        )
      }
    </QueryBoundary>
  )
}
