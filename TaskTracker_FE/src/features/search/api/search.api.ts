import { axiosClient } from '../../../api/axiosClient'
import { endpoints } from '../../../api/endpoints'
import type { GlobalSearchResult } from '../../../types/dashboard.types'

export async function globalSearch(q: string): Promise<GlobalSearchResult> {
  const { data } = await axiosClient.get<GlobalSearchResult>(endpoints.dashboard.search(), { params: { q } })
  return data
}
