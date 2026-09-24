import { axiosClient } from '../../../api/axiosClient'
import { endpoints } from '../../../api/endpoints'
import type { AccessGrant, AccessResourceType, GrantAccessRequest } from '../../../types/accessGrant.types'

export async function fetchGrantsForResource(resourceType: AccessResourceType, resourceId: number): Promise<AccessGrant[]> {
  const { data } = await axiosClient.get<AccessGrant[]>(endpoints.accessGrants.list(), { params: { resourceType, resourceId } })
  return data
}

export async function fetchSharedWithMe(): Promise<AccessGrant[]> {
  const { data } = await axiosClient.get<AccessGrant[]>(endpoints.accessGrants.mine())
  return data
}

export interface MyGrantStatus {
  viaGrant: boolean
  sharedByName: string | null
}

export async function fetchMyGrantStatus(resourceType: AccessResourceType, resourceId: number): Promise<MyGrantStatus> {
  const { data } = await axiosClient.get<MyGrantStatus>(endpoints.accessGrants.myStatus(), { params: { resourceType, resourceId } })
  return data
}

export async function fetchCanManageGrants(resourceType: AccessResourceType, resourceId: number): Promise<boolean> {
  const { data } = await axiosClient.get<{ canManage: boolean }>(endpoints.accessGrants.canManage(), { params: { resourceType, resourceId } })
  return data.canManage
}

export async function grantAccess(payload: GrantAccessRequest): Promise<AccessGrant> {
  const { data } = await axiosClient.post<AccessGrant>(endpoints.accessGrants.create(), payload)
  return data
}

export async function revokeAccess(grantId: number): Promise<void> {
  await axiosClient.delete(endpoints.accessGrants.revoke(grantId))
}
