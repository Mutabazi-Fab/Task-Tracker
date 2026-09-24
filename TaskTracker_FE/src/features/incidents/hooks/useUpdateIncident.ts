import { useMutation, useQueryClient } from '@tanstack/react-query'
import type { UpdateIncidentRequest } from '../../../types/incident.types'
import { updateIncident } from '../api/incidents.api'

export function useUpdateIncident(id: number) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: UpdateIncidentRequest) => updateIncident(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['incidents'] })
    },
  })
}
