import { axiosClient } from '../../../api/axiosClient'
import { endpoints } from '../../../api/endpoints'
import type { CreateTaskRequest, Page, TaskDetail, TaskListItem, TaskStatus } from '../../../types/task.types'

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

export async function searchTasks(q: string): Promise<TaskListItem[]> {
  const { data } = await axiosClient.get<TaskListItem[]>(endpoints.tasks.search(), { params: { q } })
  return data
}

export async function createTask(payload: CreateTaskRequest): Promise<TaskDetail> {
  const { data } = await axiosClient.post<TaskDetail>(endpoints.tasks.create(), payload)
  return data
}
