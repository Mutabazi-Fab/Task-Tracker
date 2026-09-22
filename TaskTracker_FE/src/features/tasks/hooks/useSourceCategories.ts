import { useQuery } from '@tanstack/react-query'
import { fetchSourceCategories } from '../api/sourceCategories.api'

export function useSourceCategories() {
  return useQuery({
    queryKey: ['sourceCategories'],
    queryFn: fetchSourceCategories,
  })
}
