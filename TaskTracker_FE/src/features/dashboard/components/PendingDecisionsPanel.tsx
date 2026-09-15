import { Card } from '../../../components/ui/Card'
import { EmptyState } from '../../../components/ui/EmptyState'
import { QueryBoundary } from '../../../components/feedback/QueryBoundary'
import { usePendingExtensionRequests } from '../../taskDetail/hooks/usePendingExtensionRequests'
import { PendingExtensionRequestItem } from './PendingExtensionRequestItem'
import styles from '../DashboardPage.module.css'

/** Deadline-extension requests awaiting this Executive's decision, surfaced directly on the
 *  dashboard rather than requiring a trip to the bell/Requests page — same underlying
 *  viewer-scoped data as RequestsPage (usePendingExtensionRequests), just inline. */
export function PendingDecisionsPanel() {
  const query = usePendingExtensionRequests()

  return (
    <Card>
      <div className={styles.sectionHeadingLg}>Pending decisions</div>
      <QueryBoundary query={query}>
        {(requests) =>
          requests.length === 0 ? (
            <EmptyState title="No pending decisions" description="Deadline-extension requests waiting on you show up here." />
          ) : (
            <div>
              {requests.map((request) => (
                <PendingExtensionRequestItem key={request.id} request={request} />
              ))}
            </div>
          )
        }
      </QueryBoundary>
    </Card>
  )
}
