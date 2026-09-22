import { useMutation, useQueryClient } from '@tanstack/react-query'
import { addSourceCategory } from '../api/sourceCategories.api'

export function useAddSourceCategory() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ name, addedById }: { name: string; addedById: number }) => addSourceCategory(name, addedById),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sourceCategories'] })
    },
  })
}
