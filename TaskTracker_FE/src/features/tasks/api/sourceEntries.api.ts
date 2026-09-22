import { axiosClient } from '../../../api/axiosClient'
import { endpoints } from '../../../api/endpoints'
import type { TaskSource, TaskSourceEntry } from '../../../types/task.types'

/** Open to any authenticated caller — everyone creating a task should see existing
 *  suggestions for the category they picked. */
export async function fetchSourceEntries(source: TaskSource): Promise<TaskSourceEntry[]> {
  const { data } = await axiosClient.get<TaskSourceEntry[]>(endpoints.taskSourceEntries.list(source))
  return data
}

/** Executive/Super Admin only, enforced server-side — addedById is re-derived from the
 *  caller's real login on the backend regardless of what's sent here (same "never trusted"
 *  pattern as CreateDepartmentRequest.createdById), but the field is still required by
 *  request validation, so it's included. Idempotent — adding a label that already exists
 *  (case-insensitive) for this source just returns the existing entry. */
export async function addSourceEntry(source: TaskSource, label: string, addedById: number): Promise<TaskSourceEntry> {
  const { data } = await axiosClient.post<TaskSourceEntry>(endpoints.taskSourceEntries.create(), {
    source,
    label,
    addedById,
  })
  return data
}
