import { useQuery } from '@tanstack/react-query'
import { useDebounce } from '../../../hooks/useDebounce'
import { searchTasks } from '../api/tasks.api'

/** Debounced task search by code or title. Returns the debounced text alongside the query
 *  so a caller can decide "am I searching" from the SAME value the query is enabled on —
 *  deciding that from the raw input instead opens a ~300ms window where the caller thinks
 *  a search is in flight but the query is still disabled, which crashed TaskTable before. */
export function useTaskSearch(query: string, assignedPersonId?: number) {
  const debounced = useDebounce(query.trim(), 300)

  const searchQuery = useQuery({
    queryKey: ['tasks', 'search', debounced, assignedPersonId ?? 'ALL'],
    queryFn: () => searchTasks(debounced, assignedPersonId),
    enabled: debounced.length > 0,
  })

  return { searchQuery, debouncedQuery: debounced }
}
