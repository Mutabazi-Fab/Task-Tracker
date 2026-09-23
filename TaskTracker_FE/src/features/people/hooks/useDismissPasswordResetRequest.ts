import { useMutation, useQueryClient } from '@tanstack/react-query'
import type { DismissPasswordResetRequestRequest } from '../../../types/person.types'
import { dismissPasswordResetRequest } from '../api/people.api'

export function useDismissPasswordResetRequest(personId: number) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: DismissPasswordResetRequestRequest) => dismissPasswordResetRequest(personId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['people'] })
    },
  })
}
