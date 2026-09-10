import { useQuery } from '@tanstack/react-query'
import { fetchPendingExtensionRequests } from '../api/taskDetail.api'

/** Backs the dashboard's "Requests" section — see useDecideDeadlineExtension, which
 *  invalidates this same query key so a decision made from a task's own history panel
 *  removes that row from the inbox too, and vice versa. */
export function usePendingExtensionRequests() {
  return useQuery({
    queryKey: ['pendingExtensionRequests'],
    queryFn: fetchPendingExtensionRequests,
  })
}
