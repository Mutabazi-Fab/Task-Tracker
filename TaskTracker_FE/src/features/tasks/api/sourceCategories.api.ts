import { axiosClient } from '../../../api/axiosClient'
import { endpoints } from '../../../api/endpoints'
import type { TaskSourceCategory } from '../../../types/task.types'

/** Open to any authenticated caller — everyone creating a task needs the full list for the
 *  Source dropdown. */
export async function fetchSourceCategories(): Promise<TaskSourceCategory[]> {
  const { data } = await axiosClient.get<TaskSourceCategory[]>(endpoints.taskSourceCategories.list())
  return data
}

/** Executive/Super Admin only, enforced server-side — addedById is re-derived from the caller's real
 *  login on the backend regardless of what's sent here. */
export async function addSourceCategory(name: string, addedById: number): Promise<TaskSourceCategory> {
  const { data } = await axiosClient.post<TaskSourceCategory>(endpoints.taskSourceCategories.create(), {
    name,
    addedById,
  })
  return data
}
