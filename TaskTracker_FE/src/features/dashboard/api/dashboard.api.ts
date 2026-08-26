import { axiosClient } from '../../../api/axiosClient'
import { endpoints } from '../../../api/endpoints'
import type {
  DashboardOverview,
  PersonSummary,
  ProgressPoint,
  StatusMix,
  TeamLeaderboardItem,
} from '../../../types/dashboard.types'

export async function fetchDashboardOverview(): Promise<DashboardOverview> {
  const { data } = await axiosClient.get<DashboardOverview>(endpoints.dashboard.overview())
  return data
}

export async function fetchStatusMix(): Promise<StatusMix[]> {
  const { data } = await axiosClient.get<StatusMix[]>(endpoints.dashboard.statusMix())
  return data
}

export async function fetchProgressOverTime(from: string, to: string): Promise<ProgressPoint[]> {
  const { data } = await axiosClient.get<ProgressPoint[]>(endpoints.dashboard.progressOverTime(), {
    params: { from, to },
  })
  return data
}

export async function fetchTeamLeaderboard(): Promise<TeamLeaderboardItem[]> {
  const { data } = await axiosClient.get<TeamLeaderboardItem[]>(endpoints.dashboard.teamLeaderboard())
  return data
}

export async function fetchPeopleSummary(): Promise<PersonSummary[]> {
  const { data } = await axiosClient.get<PersonSummary[]>(endpoints.dashboard.peopleSummary())
  return data
}
