import { useQuery } from '@tanstack/react-query'
import { useDebounce } from '../../../hooks/useDebounce'
import { searchTasks } from '../api/tasks.api'

/** Debounced task search by code or title, used by the in-page search box.
 *  assignedPersonId scopes results to just that person's tasks (a Member's own tasks). */
export function useTaskSearch(query: string, assignedPersonId?: number) {
  const debounced = useDebounce(query.trim(), 300)

  return useQuery({
    queryKey: ['tasks', 'search', debounced, assignedPersonId ?? 'ALL'],
    queryFn: () => searchTasks(debounced, assignedPersonId),
    enabled: debounced.length > 0,
  })
}
