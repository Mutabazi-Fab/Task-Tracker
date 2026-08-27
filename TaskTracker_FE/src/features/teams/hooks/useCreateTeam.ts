import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createTeam } from '../api/teams.api'

export function useCreateTeam() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: createTeam,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['teams'] })
      queryClient.invalidateQueries({ queryKey: ['people'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
  })
}
