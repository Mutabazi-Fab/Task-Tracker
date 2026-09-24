import { useMutation, useQueryClient } from '@tanstack/react-query'
import type { ChangeIncidentStatusRequest } from '../../../types/incident.types'
import { changeIncidentStatus } from '../api/incidents.api'

export function useChangeIncidentStatus(id: number) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: ChangeIncidentStatusRequest) => changeIncidentStatus(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['incidents'] })
    },
  })
}
