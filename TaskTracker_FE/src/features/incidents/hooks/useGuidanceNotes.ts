import { useQuery } from '@tanstack/react-query'
import { fetchGuidanceNotes } from '../api/incidentGuidance.api'

export function useGuidanceNotes() {
  return useQuery({
    queryKey: ['incidents', 'guidance-notes'],
    queryFn: fetchGuidanceNotes,
  })
}
