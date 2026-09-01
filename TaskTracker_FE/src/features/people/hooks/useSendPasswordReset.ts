import { useMutation } from '@tanstack/react-query'
import type { SendPasswordResetRequest } from '../../../types/person.types'
import { sendPasswordReset } from '../api/people.api'

export function useSendPasswordReset(personId: number) {
  return useMutation({
    mutationFn: (payload: SendPasswordResetRequest) => sendPasswordReset(personId, payload),
  })
}
