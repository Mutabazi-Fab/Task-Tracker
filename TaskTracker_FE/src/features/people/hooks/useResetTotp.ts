import { useMutation, useQueryClient } from '@tanstack/react-query'
import type { ResetTotpRequest } from '../../../types/person.types'
import { resetTotp } from '../api/people.api'

export function useResetTotp(personId: number) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: ResetTotpRequest) => resetTotp(personId, payload),
    onSuccess: () => {
      // Flips person.totpEnabled back to false — worth a refetch, unlike a password reset
      // which changes nothing visible on the Person itself.
      queryClient.invalidateQueries({ queryKey: ['people'] })
    },
  })
}
