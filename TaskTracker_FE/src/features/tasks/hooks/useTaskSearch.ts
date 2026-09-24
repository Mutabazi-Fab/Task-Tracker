import { useQuery } from '@tanstack/react-query'
import { useDebounce } from '../../../hooks/useDebounce'
import { searchTasks } from '../api/tasks.api'

/** Debounced task search by code or title. */
export function useTaskSearch(query: string, assignedPersonId?: number) {
  const debounced = useDebounce(query.trim(), 300)

  const searchQuery = useQuery({
    queryKey: ['tasks', 'search', debounced, assignedPersonId ?? 'ALL'],
    queryFn: () => searchTasks(debounced, assignedPersonId),
    enabled: debounced.length > 0,
  })

  return { searchQuery, debouncedQuery: debounced }
}
