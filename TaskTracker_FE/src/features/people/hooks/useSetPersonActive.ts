import { useMutation, useQueryClient } from '@tanstack/react-query'
import type { SetAccountActiveRequest } from '../../../types/person.types'
import { setPersonActive } from '../api/people.api'

export function useSetPersonActive(personId: number) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: SetAccountActiveRequest) => setPersonActive(personId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['people'] })
    },
  })
}
