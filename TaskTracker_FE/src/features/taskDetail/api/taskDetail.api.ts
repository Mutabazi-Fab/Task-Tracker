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

/** Only meaningful on a CEO-mandated task chain — sends a request into the CEO/Super
 *  Admin's own "Requests" inbox. No payload — the backend resolves who's forwarding it from the JWT. */
export async function forwardDeadlineExtension(taskId: number, extensionId: number): Promise<TaskDetail> {
  const { data } = await axiosClient.put<TaskDetail>(endpoints.tasks.forwardDeadlineExtension(taskId, extensionId))
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
 *  own decision. deciderId is resolved server-side from the JWT — nothing to pass here. */
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

/** Content-Type is left unset so axios generates the correct multipart boundary itself —
 *  hardcoding the header string would drop it and the backend would fail to parse the body. */
export async function addDocument(taskId: number, file: File): Promise<TaskDetail> {
  const formData = new FormData()
  formData.append('file', file)
  const { data } = await axiosClient.post<TaskDetail>(endpoints.tasks.documents(taskId), formData, {
    headers: { 'Content-Type': undefined },
  })
  return data
}

/** JWT lives in the Authorization header, not a cookie, which a plain `<a href>` link would
 *  never send — so this fetches the file as a blob through the authenticated client instead. */
export async function downloadDocument(taskId: number, documentId: number, fileName: string): Promise<void> {
  const response = await axiosClient.get(endpoints.tasks.documentDownload(taskId, documentId), {
    responseType: 'blob',
  })
  const url = URL.createObjectURL(response.data as Blob)
  const link = window.document.createElement('a')
  link.href = url
  link.download = fileName
  link.click()
  URL.revokeObjectURL(url)
}

export async function deleteDocument(taskId: number, documentId: number): Promise<TaskDetail> {
  const { data } = await axiosClient.delete<TaskDetail>(endpoints.tasks.document(taskId, documentId))
  return data
}
