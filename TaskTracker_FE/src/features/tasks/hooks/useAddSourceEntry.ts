import { useMutation, useQueryClient } from '@tanstack/react-query'
import type { TaskSource } from '../../../types/task.types'
import { addSourceEntry } from '../api/sourceEntries.api'

export function useAddSourceEntry() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ source, label, addedById }: { source: TaskSource; label: string; addedById: number }) =>
      addSourceEntry(source, label, addedById),
    onSuccess: (_entry, { source }) => {
      queryClient.invalidateQueries({ queryKey: ['sourceEntries', source] })
    },
  })
}
