import { axiosClient } from '../../../api/axiosClient'
import { endpoints } from '../../../api/endpoints'
import type {
  CreateSubtaskRequest,
  CreateTaskRequest,
  Page,
  TaskActivity,
  TaskDetail,
  TaskListItem,
  TaskSortValue,
  TaskStatus,
} from '../../../types/task.types'

export interface FetchTasksParams {
  status?: TaskStatus
  /** A Member's task list always passes their own id here — "all tasks" isn't theirs to
   *  see. Omitted for a Director/Super Admin, who see everything. */
  assignedPersonId?: number
  page: number
  size: number
  sort?: TaskSortValue
}

export async function fetchTasks({ status, assignedPersonId, page, size, sort }: FetchTasksParams): Promise<Page<TaskListItem>> {
  const { data } = await axiosClient.get<Page<TaskListItem>>(endpoints.tasks.list(), {
    // 'none' means "don't sort" — omitted entirely rather than sent as a literal value the
    // backend wouldn't know how to parse as a Spring Sort expression.
    params: { status, assignedPersonId, page, size, sort: sort === 'none' ? undefined : sort },
  })
  return data
}

// GET /tasks/search is a Page<TaskListResponse> — flattened here, same as fetchPeople,
// since this backs a live "as you type" results list, not a paged view.
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

// No body — the actor is derived from the JWT server-side, and the backend independently
// re-checks Director/Super Admin (TaskServiceImpl.deleteTask). Also recorded in the
// activity log — see fetchTaskActivity below.
export async function deleteTask(id: number): Promise<void> {
  await axiosClient.delete(endpoints.tasks.remove(id))
}

/** Director or Super Admin only — every task/subtask created or deleted, org-wide, newest
 *  first. The backend independently re-checks that tier, same as every other admin-facing
 *  read in this app. */
export async function fetchTaskActivity(page: number, size: number): Promise<Page<TaskActivity>> {
  const { data } = await axiosClient.get<Page<TaskActivity>>(endpoints.tasks.activity(), {
    params: { page, size, sort: 'timestamp,desc' },
  })
  return data
}
