import { useQuery } from '@tanstack/react-query'
import { fetchPendingExtensionRequests } from '../api/taskDetail.api'

/** Backs the Requests page and the Sidebar's own badge count on that nav item — see
 *  useDecideDeadlineExtension, which invalidates this same query key so a decision made
 *  from a task's own history panel removes that row from the inbox too, and vice versa.
 *  `enabled` defaults to true; the Sidebar passes isDirector so a plain Member (who'd only
 *  ever get an empty list back anyway, per resolveDeadlineDecider) never fires the call. */
export function usePendingExtensionRequests(enabled = true) {
  return useQuery({
    queryKey: ['pendingExtensionRequests'],
    queryFn: fetchPendingExtensionRequests,
    enabled,
  })
}
