import { useMutation, useQueryClient } from '@tanstack/react-query'
import type { ChangeRoleRequest } from '../../../types/person.types'
import { changeRole } from '../api/people.api'

export function useChangeRole(personId: number) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: ChangeRoleRequest) => changeRole(personId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['people'] })
    },
  })
}
