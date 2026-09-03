import { useQuery } from '@tanstack/react-query'
import { useDebounce } from '../../../hooks/useDebounce'
import { searchTasks } from '../api/tasks.api'

/**
 * Debounced task search by code or title, used by the in-page search box.
 * assignedPersonId scopes results to just that person's tasks (a Member's own tasks).
 *
 * Returns the debounced text alongside the query (not just the query) so a caller can
 * decide "am I searching" from the SAME value the query is enabled/disabled on. Deciding
 * that from the raw, un-debounced input instead (as TaskListPage used to) opens a ~300ms
 * window, on every keystroke, where the caller thinks a search is in flight but the query
 * is still disabled — query.isLoading is false for a disabled query with no data yet
 * (isLoading = isPending && isFetching in this version of TanStack Query), so a
 * QueryBoundary keyed off it falls through and hands a consumer `undefined` instead of
 * showing a loading state, which is exactly what crashed TaskTable.
 */
export function useTaskSearch(query: string, assignedPersonId?: number) {
  const debounced = useDebounce(query.trim(), 300)

  const searchQuery = useQuery({
    queryKey: ['tasks', 'search', debounced, assignedPersonId ?? 'ALL'],
    queryFn: () => searchTasks(debounced, assignedPersonId),
    enabled: debounced.length > 0,
  })

  return { searchQuery, debouncedQuery: debounced }
}
