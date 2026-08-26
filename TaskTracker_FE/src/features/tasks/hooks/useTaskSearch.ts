import { useQuery } from '@tanstack/react-query'
import { useDebounce } from '../../../hooks/useDebounce'
import { searchTasks } from '../api/tasks.api'

/** Debounced task search by code or title, used by the in-page search box. */
export function useTaskSearch(query: string) {
  const debounced = useDebounce(query.trim(), 300)

  return useQuery({
    queryKey: ['tasks', 'search', debounced],
    queryFn: () => searchTasks(debounced),
    enabled: debounced.length > 0,
  })
}
