import { useMutation, useQueryClient } from '@tanstack/react-query'
import { deleteDocument } from '../api/taskDetail.api'

export function useDeleteDocument(taskId: number) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (documentId: number) => deleteDocument(taskId, documentId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks'] })
    },
  })
}
