import { axiosClient } from '../../../api/axiosClient'
import { endpoints } from '../../../api/endpoints'
import type { Page } from '../../../types/task.types'
import type { Person, PersonStatistics, PersonTaskHistoryItem } from '../../../types/person.types'

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
