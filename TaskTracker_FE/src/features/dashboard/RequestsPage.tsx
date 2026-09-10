import { Navigate } from 'react-router-dom'
import { PageHeader } from '../../components/layout/PageHeader'
import { Card } from '../../components/ui/Card'
import { EmptyState } from '../../components/ui/EmptyState'
import { QueryBoundary } from '../../components/feedback/QueryBoundary'
import { ROUTES } from '../../app/routePaths'
import { useAuth } from '../auth/useAuth'
import { usePendingExtensionRequests } from '../taskDetail/hooks/usePendingExtensionRequests'
import { PendingExtensionRequestItem } from './components/PendingExtensionRequestItem'

/**
 * Its own sidebar destination rather than a section buried on the dashboard — every
 * deadline-extension request still waiting on THIS viewer's decision, across every task
 * they're the decider for. Director or Super Admin only, same tier as Activity/Departments
 * (a plain Member is never a decider — see resolveDeadlineDecider on the backend — so this
 * would always be empty for them); redirects away outright if landed on directly, same
 * pattern as ActivityPage.
 */
export function RequestsPage() {
  const { isDirector } = useAuth()
  const query = usePendingExtensionRequests()

  if (!isDirector) {
    return <Navigate to={ROUTES.dashboard} replace />
  }

  return (
    <>
      <PageHeader breadcrumb="Throughline" title="Requests" />
      <Card>
        <QueryBoundary query={query}>
          {(requests) =>
            requests.length === 0 ? (
              <EmptyState title="No pending requests" description="Deadline-extension requests waiting on you show up here." />
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
    </>
  )
}
