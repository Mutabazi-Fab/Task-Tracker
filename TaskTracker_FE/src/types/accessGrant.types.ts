/** RISK is reserved for the risk register, which doesn't exist yet. */
export type AccessResourceType = 'TASK' | 'INCIDENT' | 'RISK'

export interface AccessGrant {
  id: number
  granteeId: number
  granteeName: string
  granteeJobTitle: string
  resourceType: AccessResourceType
  resourceId: number
  resourceCode: string | null
  resourceTitle: string | null
  grantedByName: string
  reason: string
  grantedAt: string
}

/** Body for POST /access-grants. Executive/Super Admin only; grantedById is overwritten server-side. */
export interface GrantAccessRequest {
  resourceType: AccessResourceType
  resourceId: number
  granteeId: number
  reason: string
  grantedById: number
}
