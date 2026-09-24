import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { fetchIncidents, type FetchIncidentsParams } from '../api/incidents.api'

export function useIncidents(params: FetchIncidentsParams) {
  return useQuery({
    queryKey: ['incidents', 'list', params],
    queryFn: () => fetchIncidents(params),
    placeholderData: keepPreviousData,
  })
}
