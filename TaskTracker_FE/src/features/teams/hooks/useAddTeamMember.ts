import { useMutation, useQueryClient } from '@tanstack/react-query'
import type { AddTeamMemberRequest } from '../../../types/team.types'
import { addTeamMember } from '../api/teams.api'

export function useAddTeamMember(teamId: number) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: AddTeamMemberRequest) => addTeamMember(teamId, payload),
    onSuccess: () => {
      // Also creates a Notification for the team's Director (unless they made the change
      // themself) — refresh that badge/list too, not just team/people state.
      queryClient.invalidateQueries({ queryKey: ['teams'] })
      queryClient.invalidateQueries({ queryKey: ['people'] })
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
    },
  })
}
