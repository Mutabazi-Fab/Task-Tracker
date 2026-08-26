import { axiosClient } from '../../../api/axiosClient'
import { endpoints } from '../../../api/endpoints'
import type { Person } from '../../../types/person.types'
import type { TaskListItem } from '../../../types/task.types'
import type { Team, TeamStatistics } from '../../../types/team.types'

export async function fetchTeams(): Promise<Team[]> {
  const { data } = await axiosClient.get<Team[]>(endpoints.teams.list())
  return data
}

export async function fetchTeam(id: number): Promise<Team> {
  const { data } = await axiosClient.get<Team>(endpoints.teams.detail(id))
  return data
}

export async function fetchTeamStatistics(id: number): Promise<TeamStatistics> {
  const { data } = await axiosClient.get<TeamStatistics>(endpoints.teams.statistics(id))
  return data
}

export async function fetchTeamTasks(id: number): Promise<TaskListItem[]> {
  const { data } = await axiosClient.get<TaskListItem[]>(endpoints.teams.tasks(id))
  return data
}

/**
 * TeamResponse/TeamStatisticsResponse never carry member ids — only
 * memberCount and memberProgresses[].name — so there is no endpoint that
 * returns "this team's members" as linkable {id, name} pairs. This filters
 * the full person list client-side, which is the only way to get one.
 */
export async function fetchTeamMembers(teamId: number): Promise<Person[]> {
  const { data } = await axiosClient.get<Person[]>(endpoints.people.list())
  return data.filter((person) => person.teamId === teamId)
}
