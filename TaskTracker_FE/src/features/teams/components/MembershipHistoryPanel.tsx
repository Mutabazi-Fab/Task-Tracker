import { QueryBoundary } from '../../../components/feedback/QueryBoundary'
import { EmptyState } from '../../../components/ui/EmptyState'
import { formatDateTime } from '../../../lib/formatDate'
import { useMembershipHistory } from '../hooks/useMembershipHistory'
import styles from './MembershipHistoryPanel.module.css'

/** Append-only audit log of every add/remove for this team, newest first — never edited,
 *  survives the current roster moving on. */
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
                  <span className={change.action === 'ADDED' ? styles.actionAdded : styles.actionRemoved}>
                    {change.action === 'ADDED' ? 'Added' : 'Removed'}
                  </span>{' '}
                  {change.personName} — {change.reason}
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
