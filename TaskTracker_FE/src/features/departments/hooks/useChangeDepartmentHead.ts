import { useMutation, useQueryClient } from '@tanstack/react-query'
import { changeDepartmentHead } from '../api/departments.api'
import type { ChangeDepartmentHeadRequest } from '../../../types/department.types'

export function useChangeDepartmentHead(id: number) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: ChangeDepartmentHeadRequest) => changeDepartmentHead(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['departments'] })
    },
  })
}
