import { axiosClient } from '../../../api/axiosClient'
import { endpoints } from '../../../api/endpoints'
import type { Page } from '../../../types/task.types'
import type {
  AccountStatusChangeActivity,
  ChangeRoleRequest,
  CreatePersonRequest,
  DismissPasswordResetRequestRequest,
  Person,
  PersonStatistics,
  PersonTaskHistoryItem,
  ResetTotpRequest,
  RoleChangeActivity,
  SetAccountActiveRequest,
  SetPasswordRequest,
} from '../../../types/person.types'

/** GET /people returns a Spring Data Page<PersonResponse> — a large page size is passed so
 *  this keeps behaving like "the whole org" for pickers/lists that expect a flat array. */
export async function fetchPeople(): Promise<Person[]> {
  const { data } = await axiosClient.get<Page<Person>>(endpoints.people.list(), { params: { size: 200 } })
  return data.content
}

export async function fetchPerson(id: number): Promise<Person> {
  const { data } = await axiosClient.get<Person>(endpoints.people.detail(id))
  return data
}

export async function fetchPersonStatistics(id: number): Promise<PersonStatistics> {
  const { data } = await axiosClient.get<PersonStatistics>(endpoints.people.statistics(id))
  return data
}

/** GET /people/{id}/tasks is also a Page<PersonTaskHistoryResponse> now — same "flatten it
 *  for now" approach as fetchPeople above. */
export async function fetchPersonTaskHistory(id: number): Promise<PersonTaskHistoryItem[]> {
  const { data } = await axiosClient.get<Page<PersonTaskHistoryItem>>(endpoints.people.tasks(id), {
    params: { size: 200 },
  })
  return data.content
}

/** Super-Admin-only, enforced server-side — there is no public self-registration. */
export async function createPerson(payload: CreatePersonRequest): Promise<Person> {
  const { data } = await axiosClient.post<Person>(endpoints.people.create(), payload)
  return data
}

/** Super-Admin-only, enforced server-side. */
export async function changeRole(id: number, payload: ChangeRoleRequest): Promise<Person> {
  const { data } = await axiosClient.put<Person>(endpoints.people.changeRole(id), payload)
  return data
}

/** Super-Admin-only, enforced server-side. */
export async function setPersonActive(id: number, payload: SetAccountActiveRequest): Promise<Person> {
  const { data } = await axiosClient.put<Person>(endpoints.people.setActive(id), payload)
  return data
}

/** Super-Admin-only — every role change ever made, org-wide, newest first. */
export async function fetchRoleChangeActivity(requesterId: number): Promise<RoleChangeActivity[]> {
  const { data } = await axiosClient.get<Page<RoleChangeActivity>>(endpoints.people.roleChanges(), {
    params: { requesterId, size: 100, sort: 'timestamp,desc' },
  })
  return data.content
}

/** Super-Admin-only, enforced server-side. */
export async function setPassword(id: number, payload: SetPasswordRequest): Promise<void> {
  await axiosClient.post(endpoints.people.setPassword(id), payload)
}

/** Super-Admin-only, enforced server-side. Dismisses a pending password-reset request
 *  without changing the person's password. */
export async function dismissPasswordResetRequest(id: number, payload: DismissPasswordResetRequestRequest): Promise<void> {
  await axiosClient.post(endpoints.people.dismissPasswordResetRequest(id), payload)
}

/** Super-Admin-only, enforced server-side — for a lost/replaced phone. */
export async function resetTotp(id: number, payload: ResetTotpRequest): Promise<void> {
  await axiosClient.post(endpoints.people.resetTotp(id), payload)
}

/** Super-Admin-only — every account activation/deactivation ever made, org-wide, newest
 *  first. */
export async function fetchAccountStatusChangeActivity(requesterId: number): Promise<AccountStatusChangeActivity[]> {
  const { data } = await axiosClient.get<Page<AccountStatusChangeActivity>>(endpoints.people.accountStatusChanges(), {
    params: { requesterId, size: 100, sort: 'timestamp,desc' },
  })
  return data.content
}

/** Self-only, enforced server-side — taskId must be one of the caller's own assigned tasks. */
export async function addDailyGoal(personId: number, taskId: number): Promise<PersonStatistics> {
  const { data } = await axiosClient.post<PersonStatistics>(endpoints.people.addDailyGoal(personId), { taskId })
  return data
}

/** Self-only, enforced server-side. Silently fine if the task wasn't a daily goal already. */
export async function removeDailyGoal(personId: number, taskId: number): Promise<PersonStatistics> {
  const { data } = await axiosClient.delete<PersonStatistics>(endpoints.people.removeDailyGoal(personId, taskId))
  return data
}
