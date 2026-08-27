import { useMutation, useQueryClient } from '@tanstack/react-query'
import { setTeamLeader } from '../api/teams.api'

export function useSetTeamLeader(teamId: number) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ personId, changedById }: { personId: number; changedById: number }) =>
      setTeamLeader(teamId, personId, { changedById }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['teams'] })
      queryClient.invalidateQueries({ queryKey: ['people'] })
    },
  })
}
