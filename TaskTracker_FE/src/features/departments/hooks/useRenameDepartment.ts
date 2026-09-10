import { useMutation, useQueryClient } from '@tanstack/react-query'
import { renameDepartment } from '../api/departments.api'
import type { RenameDepartmentRequest } from '../../../types/department.types'

export function useRenameDepartment(id: number) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: RenameDepartmentRequest) => renameDepartment(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['departments'] })
    },
  })
}
