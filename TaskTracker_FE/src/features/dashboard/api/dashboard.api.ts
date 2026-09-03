import { axiosClient } from '../../../api/axiosClient'
import { endpoints } from '../../../api/endpoints'
import type {
  DashboardOverview,
  PersonSummary,
  ProgressPoint,
  StatusMix,
  TeamLeaderboardItem,
} from '../../../types/dashboard.types'
import type { Page, TaskListItem } from '../../../types/task.types'

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

/** Only the top-level tasks THIS Director created — not the whole org's. Director/Super
 *  Admin only; the backend rejects anyone else. */
export async function fetchDirectorTasks(directorId: number, page: number, size: number): Promise<Page<TaskListItem>> {
  const { data } = await axiosClient.get<Page<TaskListItem>>(endpoints.dashboard.directorTasks(), {
    params: { directorId, page, size },
  })
  return data
}
