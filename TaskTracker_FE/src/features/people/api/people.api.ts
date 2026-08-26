import { axiosClient } from '../../../api/axiosClient'
import { endpoints } from '../../../api/endpoints'
import type { Person, PersonStatistics, PersonTaskHistoryItem } from '../../../types/person.types'

export async function fetchPeople(): Promise<Person[]> {
  const { data } = await axiosClient.get<Person[]>(endpoints.people.list())
  return data
}

export async function fetchPerson(id: number): Promise<Person> {
  const { data } = await axiosClient.get<Person>(endpoints.people.detail(id))
  return data
}

export async function fetchPersonStatistics(id: number): Promise<PersonStatistics> {
  const { data } = await axiosClient.get<PersonStatistics>(endpoints.people.statistics(id))
  return data
}

export async function fetchPersonTaskHistory(id: number): Promise<PersonTaskHistoryItem[]> {
  const { data } = await axiosClient.get<PersonTaskHistoryItem[]>(endpoints.people.tasks(id))
  return data
}
