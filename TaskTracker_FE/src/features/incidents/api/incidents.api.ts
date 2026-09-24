import { axiosClient } from '../../../api/axiosClient'
import { endpoints } from '../../../api/endpoints'
import type {
  ChangeIncidentStatusRequest,
  CreateIncidentRequest,
  IncidentBusinessUnit,
  IncidentCategory,
  IncidentDashboard,
  IncidentDetail,
  IncidentListItem,
  IncidentSeverity,
  IncidentStatus,
  Page,
  UpdateIncidentRequest,
} from '../../../types/incident.types'

export interface FetchIncidentsParams {
  status?: IncidentStatus
  severity?: IncidentSeverity
  category?: IncidentCategory
  businessUnit?: IncidentBusinessUnit
  from?: string
  to?: string
  q?: string
  page: number
  size: number
  sort?: string
}

export async function fetchIncidents(params: FetchIncidentsParams): Promise<Page<IncidentListItem>> {
  const { data } = await axiosClient.get<Page<IncidentListItem>>(endpoints.incidents.list(), { params })
  return data
}

export async function fetchIncidentById(id: number): Promise<IncidentDetail> {
  const { data } = await axiosClient.get<IncidentDetail>(endpoints.incidents.detail(id))
  return data
}

export async function createIncident(payload: CreateIncidentRequest): Promise<IncidentDetail> {
  const { data } = await axiosClient.post<IncidentDetail>(endpoints.incidents.create(), payload)
  return data
}

export async function updateIncident(id: number, payload: UpdateIncidentRequest): Promise<IncidentDetail> {
  const { data } = await axiosClient.put<IncidentDetail>(endpoints.incidents.update(id), payload)
  return data
}

export async function changeIncidentStatus(id: number, payload: ChangeIncidentStatusRequest): Promise<IncidentDetail> {
  const { data } = await axiosClient.put<IncidentDetail>(endpoints.incidents.changeStatus(id), payload)
  return data
}

export async function fetchIncidentDashboard(): Promise<IncidentDashboard> {
  const { data } = await axiosClient.get<IncidentDashboard>(endpoints.incidents.dashboard())
  return data
}
