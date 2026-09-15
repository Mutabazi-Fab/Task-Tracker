import { axiosClient } from '../../../api/axiosClient'
import { endpoints } from '../../../api/endpoints'
import type {
  DashboardOverview,
  DepartmentHealth,
  ExecutiveKpi,
  PersonSummary,
  ProgressPoint,
  StatusMix,
  TeamLeaderboardItem,
} from '../../../types/dashboard.types'
import type { Page, TaskListItem, TaskSortValue } from '../../../types/task.types'

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
export async function fetchDirectorTasks(
  directorId: number,
  page: number,
  size: number,
  sort: TaskSortValue,
): Promise<Page<TaskListItem>> {
  const { data } = await axiosClient.get<Page<TaskListItem>>(endpoints.dashboard.directorTasks(), {
    // 'none' means "don't sort" — omitted entirely, same as fetchTasks.
    params: { directorId, page, size, sort: sort === 'none' ? undefined : sort },
  })
  return data
}

/** Every CRITICAL-severity task org-wide (any depth) plus every task an Executive/Super
 *  Admin personally assigned — not top-level tasks any more, not scoped to just this
 *  particular Executive's own. Executive/Super Admin only; the backend rejects anyone else.
 *  No viewer id to pass — unlike fetchDirectorTasks, there's no "whose" to scope this to. */
export async function fetchExecutiveTasks(page: number, size: number, sort: TaskSortValue): Promise<Page<TaskListItem>> {
  const { data } = await axiosClient.get<Page<TaskListItem>>(endpoints.dashboard.executiveTasks(), {
    params: { page, size, sort: sort === 'none' ? undefined : sort },
  })
  return data
}

/** The Executive Dashboard's department traffic-light roll-up. Executive/Super Admin only. */
export async function fetchExecutiveDepartmentHealth(): Promise<DepartmentHealth[]> {
  const { data } = await axiosClient.get<DepartmentHealth[]>(endpoints.dashboard.executiveDepartmentHealth())
  return data
}

/** The Executive Dashboard's four org-health KPI tiles. Executive/Super Admin only. */
export async function fetchExecutiveKpis(): Promise<ExecutiveKpi> {
  const { data } = await axiosClient.get<ExecutiveKpi>(endpoints.dashboard.executiveKpis())
  return data
}
