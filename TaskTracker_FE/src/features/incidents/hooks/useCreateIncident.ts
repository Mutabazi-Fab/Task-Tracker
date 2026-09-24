import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createIncident } from '../api/incidents.api'

export function useCreateIncident() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: createIncident,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['incidents'] })
    },
  })
}
