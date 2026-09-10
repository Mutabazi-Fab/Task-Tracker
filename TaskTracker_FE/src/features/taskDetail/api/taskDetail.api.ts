import { axiosClient } from '../../../api/axiosClient'
import { endpoints } from '../../../api/endpoints'
import type { AddCommentRequest, AddDiscussionCommentRequest } from '../../../types/comment.types'
import type { ReassignTaskRequest } from '../../../types/reassignment.types'
import type { SetPinnedRequest, TaskDetail } from '../../../types/task.types'
import type {
  DecideDeadlineExtensionRequest,
  ExtendDeadlineRequest,
  PendingExtensionRequest,
  RequestDeadlineExtensionRequest,
} from '../../../types/deadlineExtension.types'

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

export async function requestDeadlineExtension(
  taskId: number,
  payload: RequestDeadlineExtensionRequest,
): Promise<TaskDetail> {
  const { data } = await axiosClient.post<TaskDetail>(endpoints.tasks.deadlineExtensions(taskId), payload)
  return data
}

export async function decideDeadlineExtension(
  taskId: number,
  extensionId: number,
  payload: DecideDeadlineExtensionRequest,
): Promise<TaskDetail> {
  const { data } = await axiosClient.put<TaskDetail>(
    endpoints.tasks.decideDeadlineExtension(taskId, extensionId),
    payload,
  )
  return data
}

export async function extendDeadlineDirectly(taskId: number, payload: ExtendDeadlineRequest): Promise<TaskDetail> {
  const { data } = await axiosClient.put<TaskDetail>(endpoints.tasks.deadline(taskId), payload)
  return data
}

/** The "Requests" inbox — every deadline-extension request still waiting on the caller's
 *  own decision, across every task. deciderId is resolved server-side from the JWT, same
 *  as everywhere else — nothing to pass here. */
export async function fetchPendingExtensionRequests(): Promise<PendingExtensionRequest[]> {
  const { data } = await axiosClient.get<PendingExtensionRequest[]>(endpoints.tasks.pendingDeadlineExtensions())
  return data
}

export async function setPinned(taskId: number, payload: SetPinnedRequest): Promise<TaskDetail> {
  const { data } = await axiosClient.put<TaskDetail>(endpoints.tasks.pin(taskId), payload)
  return data
}

/** A plain Q&A message, fully open — never touches percentage/status. */
export async function addDiscussionComment(taskId: number, payload: AddDiscussionCommentRequest): Promise<TaskDetail> {
  const { data } = await axiosClient.post<TaskDetail>(endpoints.tasks.discussionComments(taskId), payload)
  return data
}
