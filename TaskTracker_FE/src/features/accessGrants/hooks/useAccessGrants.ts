import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { AccessResourceType, GrantAccessRequest } from '../../../types/accessGrant.types'
import { fetchCanManageGrants, fetchGrantsForResource, fetchMyGrantStatus, fetchSharedWithMe, grantAccess, revokeAccess } from '../api/accessGrants.api'

const KEY = ['access-grants']

/** Who has been given access to this item. Only ever requested by an Executive/Super Admin. */
export function useGrantsForResource(resourceType: AccessResourceType, resourceId: number, enabled: boolean) {
  return useQuery({
    queryKey: [...KEY, 'resource', resourceType, resourceId],
    queryFn: () => fetchGrantsForResource(resourceType, resourceId),
    enabled,
  })
}

/** May the signed-in person share this item and remove access to it? True for an Executive/Super Admin, and for a
 *  Director on an item from their own department. Only asked of a Director-or-above. */
export function useCanManageGrants(resourceType: AccessResourceType, resourceId: number, enabled: boolean) {
  return useQuery({
    queryKey: [...KEY, 'can-manage', resourceType, resourceId],
    queryFn: () => fetchCanManageGrants(resourceType, resourceId),
    enabled,
  })
}

export function useSharedWithMe() {
  return useQuery({ queryKey: [...KEY, 'mine'], queryFn: fetchSharedWithMe })
}

/** Does the signed-in person currently have a grant on this item? Drives the "not from your department" banner. */
export function useMyGrantStatus(resourceType: AccessResourceType, resourceId: number, enabled: boolean) {
  return useQuery({
    queryKey: [...KEY, 'status', resourceType, resourceId],
    queryFn: () => fetchMyGrantStatus(resourceType, resourceId),
    enabled,
  })
}

export function useGrantAccess() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: GrantAccessRequest) => grantAccess(payload),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: KEY }),
  })
}

export function useRevokeAccess() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (grantId: number) => revokeAccess(grantId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: KEY }),
  })
}
