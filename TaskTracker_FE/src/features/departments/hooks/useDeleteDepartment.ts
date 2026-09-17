import { useMutation, useQueryClient } from '@tanstack/react-query'
import { deleteDepartment } from '../api/departments.api'

export function useDeleteDepartment() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: number) => deleteDepartment(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['departments'] })
    },
  })
}
