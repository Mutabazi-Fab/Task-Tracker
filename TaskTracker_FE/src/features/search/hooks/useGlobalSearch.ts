import { useQuery } from '@tanstack/react-query'
import { useDebounce } from '../../../hooks/useDebounce'
import { globalSearch } from '../api/search.api'

export function useGlobalSearch(query: string) {
  const debounced = useDebounce(query.trim(), 300)

  return useQuery({
    queryKey: ['search', 'global', debounced],
    queryFn: () => globalSearch(debounced),
    enabled: debounced.length > 0,
  })
}
