import { useQuery } from '@tanstack/react-query'
import { fetchPendingExtensionRequests } from '../api/taskDetail.api'

/** Backs the Requests page and the Sidebar's own badge count on that nav item — see
 *  useDecideDeadlineExtension, which invalidates this same query key so a decision made from a task's
 *  own history panel removes that row from the inbox too, and vice versa. */
export function usePendingExtensionRequests(enabled = true) {
  return useQuery({
    queryKey: ['pendingExtensionRequests'],
    queryFn: fetchPendingExtensionRequests,
    enabled,
  })
}
