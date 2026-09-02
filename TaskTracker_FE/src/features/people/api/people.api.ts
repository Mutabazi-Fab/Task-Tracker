import { axiosClient } from '../../../api/axiosClient'
import { endpoints } from '../../../api/endpoints'
import type { Page } from '../../../types/task.types'
import type {
  AccountStatusChangeActivity,
  ChangeRoleRequest,
  CreatePersonRequest,
  Person,
  PersonStatistics,
  PersonTaskHistoryItem,
  RoleChangeActivity,
  SendPasswordResetRequest,
  SetAccountActiveRequest,
} from '../../../types/person.types'

/**
 * GET /people now returns a Spring Data Page<PersonResponse>, not a bare array — a large
 * page size is passed so this keeps behaving like "the whole org" for the pickers/lists
 * that expect a flat array, same as before pagination existed on the backend. A real
 * pager (prev/next, page size control) is a separate, later concern for whichever page
 * actually needs one.
 */
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

/** Director/Super-Admin-only, enforced server-side. Sends a best-effort invite email. */
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

/** Super-Admin-only, enforced server-side. Fails if this person has never signed up. */
export async function sendPasswordReset(id: number, payload: SendPasswordResetRequest): Promise<void> {
  await axiosClient.post(endpoints.people.sendPasswordReset(id), payload)
}

/** Super-Admin-only — every account activation/deactivation ever made, org-wide, newest
 *  first. */
export async function fetchAccountStatusChangeActivity(requesterId: number): Promise<AccountStatusChangeActivity[]> {
  const { data } = await axiosClient.get<Page<AccountStatusChangeActivity>>(endpoints.people.accountStatusChanges(), {
    params: { requesterId, size: 100, sort: 'timestamp,desc' },
  })
  return data.content
}
