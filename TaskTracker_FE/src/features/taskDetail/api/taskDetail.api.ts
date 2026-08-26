import { axiosClient } from '../../../api/axiosClient'
import { endpoints } from '../../../api/endpoints'
import type { AddCommentRequest } from '../../../types/comment.types'
import type { ReassignTaskRequest } from '../../../types/reassignment.types'
import type { TaskDetail } from '../../../types/task.types'

export async function fetchTaskDetail(taskId: number): Promise<TaskDetail> {
  const { data } = await axiosClient.get<TaskDetail>(endpoints.tasks.detail(taskId))
  return data
}

/** The only way progress ever changes — no endpoint sets progress directly. */
export async function addComment(taskId: number, payload: AddCommentRequest): Promise<TaskDetail> {
  const { data } = await axiosClient.post<TaskDetail>(endpoints.tasks.addComment(taskId), payload)
  return data
}

export async function reassignTask(taskId: number, payload: ReassignTaskRequest): Promise<TaskDetail> {
  const { data } = await axiosClient.post<TaskDetail>(endpoints.tasks.reassign(taskId), payload)
  return data
}
