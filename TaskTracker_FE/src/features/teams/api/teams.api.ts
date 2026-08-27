import { axiosClient } from '../../../api/axiosClient'
import { endpoints } from '../../../api/endpoints'
import type { Page } from '../../../types/task.types'
import type {
  AddTeamMemberRequest,
  CreateTeamRequest,
  RemoveTeamMemberRequest,
  SetTeamLeaderRequest,
  Team,
  TeamMember,
  TeamMembershipChange,
  TeamStatistics,
} from '../../../types/team.types'
import type { TaskListItem } from '../../../types/task.types'

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

export async function fetchTeamMembers(teamId: number): Promise<TeamMember[]> {
  const { data } = await axiosClient.get<TeamMember[]>(endpoints.teams.members(teamId))
  return data
}

export async function createTeam(payload: CreateTeamRequest): Promise<Team> {
  const { data } = await axiosClient.post<Team>(endpoints.teams.create(), payload)
  return data
}

export async function addTeamMember(teamId: number, payload: AddTeamMemberRequest): Promise<Team> {
  const { data } = await axiosClient.post<Team>(endpoints.teams.addMember(teamId), payload)
  return data
}

export async function removeTeamMember(
  teamId: number,
  personId: number,
  payload: RemoveTeamMemberRequest,
): Promise<Team> {
  const { data } = await axiosClient.delete<Team>(endpoints.teams.removeMember(teamId, personId), {
    data: payload,
  })
  return data
}

export async function setTeamLeader(
  teamId: number,
  personId: number,
  payload: SetTeamLeaderRequest,
): Promise<Team> {
  const { data } = await axiosClient.put<Team>(endpoints.teams.setLeader(teamId, personId), payload)
  return data
}

/** GET /teams/{id}/membership-history — a large page size flattens it into one list,
 *  same trade-off as fetchPeople: this is a compact audit feed on the team page, not a
 *  separate paged view (yet). */
export async function fetchMembershipHistory(teamId: number): Promise<TeamMembershipChange[]> {
  const { data } = await axiosClient.get<Page<TeamMembershipChange>>(endpoints.teams.membershipHistory(teamId), {
    params: { size: 50, sort: 'timestamp,desc' },
  })
  return data.content
}
