import { useQuery } from '@tanstack/react-query'
import { useDebounce } from '../../../hooks/useDebounce'
import { globalSearch } from '../api/search.api'

/** Returns the debounced text alongside the query — see useTaskSearch's doc comment for
 *  why a caller (SearchInput's live dropdown) needs to key its own "do I have a search in
 *  flight" state off this SAME debounced value, not the raw input, to avoid a ~300ms
 *  window on every keystroke where it thinks a search is active but the query is still
 *  disabled. */
export function useGlobalSearch(query: string) {
  const debounced = useDebounce(query.trim(), 300)

  const searchQuery = useQuery({
    queryKey: ['search', 'global', debounced],
    queryFn: () => globalSearch(debounced),
    enabled: debounced.length > 0,
  })

  return { searchQuery, debouncedQuery: debounced }
}
