import { useQuery } from '@tanstack/react-query'
import { fetchDepartments } from '../api/departments.api'

/** enabled defaults true for existing callers (the various "pick a department" pickers, which are
 *  open-read forms anyway) — pass false to skip fetching when the caller doesn't need the list yet
 *  (e.g. */
export function useDepartments(enabled: boolean = true) {
  return useQuery({
    queryKey: ['departments', 'list'],
    queryFn: fetchDepartments,
    enabled,
  })
}
