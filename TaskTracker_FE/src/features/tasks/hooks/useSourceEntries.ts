import { useQuery } from '@tanstack/react-query'
import type { TaskSource } from '../../../types/task.types'
import { fetchSourceEntries } from '../api/sourceEntries.api'

export function useSourceEntries(source: TaskSource | '') {
  return useQuery({
    queryKey: ['sourceEntries', source],
    queryFn: () => fetchSourceEntries(source as TaskSource),
    enabled: source !== '',
  })
}
