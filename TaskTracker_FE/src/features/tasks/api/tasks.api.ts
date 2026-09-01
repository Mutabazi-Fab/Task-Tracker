import { axiosClient } from '../../../api/axiosClient'
import { endpoints } from '../../../api/endpoints'
import type {
  CreateSubtaskRequest,
  CreateTaskRequest,
  Page,
  TaskDetail,
  TaskListItem,
  TaskStatus,
} from '../../../types/task.types'

export interface FetchTasksParams {
  status?: TaskStatus
  page: number
  size: number
}

export async function fetchTasks({ status, page, size }: FetchTasksParams): Promise<Page<TaskListItem>> {
  const { data } = await axiosClient.get<Page<TaskListItem>>(endpoints.tasks.list(), {
    params: { status, page, size },
  })
  return data
}

// GET /tasks/search is a Page<TaskListResponse> now — flattened here for the same reason
// as fetchPeople in people.api.ts (this is used for a live "as you type" results list, not
// a paged view).
export async function searchTasks(q: string): Promise<TaskListItem[]> {
  const { data } = await axiosClient.get<Page<TaskListItem>>(endpoints.tasks.search(), {
    params: { q, size: 50 },
  })
  return data.content
}

export async function createTask(payload: CreateTaskRequest): Promise<TaskDetail> {
  const { data } = await axiosClient.post<TaskDetail>(endpoints.tasks.create(), payload)
  return data
}

export async function createSubtask(parentTaskId: number, payload: CreateSubtaskRequest): Promise<TaskDetail> {
  const { data } = await axiosClient.post<TaskDetail>(endpoints.tasks.subtasks(parentTaskId), payload)
  return data
}

// No actor/reason in the body — the backend doesn't take one for this endpoint (a real,
// tracked gap: DELETE /tasks/{id} isn't role-gated server-side yet, unlike creation). The
// button that calls this is still gated client-side to Director/Super Admin or the owning
// team's leader, matching who can create a task, so this stays consistent in normal use.
export async function deleteTask(id: number): Promise<void> {
  await axiosClient.delete(endpoints.tasks.remove(id))
}
