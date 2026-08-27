import { useMutation, useQueryClient } from '@tanstack/react-query'
import type { RemoveTeamMemberRequest } from '../../../types/team.types'
import { removeTeamMember } from '../api/teams.api'

export function useRemoveTeamMember(teamId: number) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ personId, ...payload }: RemoveTeamMemberRequest & { personId: number }) =>
      removeTeamMember(teamId, personId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['teams'] })
      queryClient.invalidateQueries({ queryKey: ['people'] })
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
    },
  })
}
