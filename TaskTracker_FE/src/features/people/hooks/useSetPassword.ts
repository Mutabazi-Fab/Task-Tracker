import { useMutation, useQueryClient } from '@tanstack/react-query'
import type { SetPasswordRequest } from '../../../types/person.types'
import { setPassword } from '../api/people.api'

export function useSetPassword(personId: number) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: SetPasswordRequest) => setPassword(personId, payload),
    onSuccess: () => {
      // Auto-resolves any pending password-reset request server-side — worth a refetch so
      // the pending-request banner in PersonAdminControls disappears immediately.
      queryClient.invalidateQueries({ queryKey: ['people'] })
    },
  })
}
