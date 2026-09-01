import { useQuery } from '@tanstack/react-query'
import { fetchPeople } from '../api/people.api'

/** enabled defaults true for existing callers (the various "pick a person" pickers, which
 *  are Director/Super-Admin-only forms anyway) — PeopleListPage passes false for a Member,
 *  so the whole-org roster is never even fetched for someone who isn't allowed to see it,
 *  not just hidden in the UI. */
export function usePeople(enabled: boolean = true) {
  return useQuery({
    queryKey: ['people', 'list'],
    queryFn: fetchPeople,
    enabled,
  })
}
