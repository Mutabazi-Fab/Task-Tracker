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
  /** A Member's task list always passes their own id here — "all tasks" isn't theirs to
   *  see. Omitted for a Director/Super Admin, who see everything. */
  assignedPersonId?: number
  page: number
  size: number
}

export async function fetchTasks({ status, assignedPersonId, page, size }: FetchTasksParams): Promise<Page<TaskListItem>> {
  const { data } = await axiosClient.get<Page<TaskListItem>>(endpoints.tasks.list(), {
    params: { status, assignedPersonId, page, size },
  })
  return data
}

// GET /tasks/search is a Page<TaskListResponse> now — flattened here for the same reason
// as fetchPeople in people.api.ts (this is used for a live "as you type" results list, not
// a paged view). assignedPersonId scopes it the same way fetchTasks does, for a Member.
export async function searchTasks(q: string, assignedPersonId?: number): Promise<TaskListItem[]> {
  const { data } = await axiosClient.get<Page<TaskListItem>>(endpoints.tasks.search(), {
    params: { q, assignedPersonId, size: 50 },
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
