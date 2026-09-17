import { useMutation, useQueryClient } from '@tanstack/react-query'
import { addDocument } from '../api/taskDetail.api'

export function useAddDocument(taskId: number) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (file: File) => addDocument(taskId, file),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks'] })
    },
  })
}
