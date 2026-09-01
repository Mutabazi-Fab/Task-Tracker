import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createPerson } from '../api/people.api'

export function useCreatePerson() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: createPerson,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['people'] })
    },
  })
}
