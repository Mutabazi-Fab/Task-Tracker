import { useAuth } from '../../auth/useAuth'
import { useMyGrantStatus } from '../hooks/useAccessGrants'
import type { AccessResourceType } from '../../../types/accessGrant.types'
import styles from './AccessGrants.module.css'

/** Shown to someone who can open this task or incident only because it was shared with them, naming who shared
 *  it. Executives and Super Admins see everything anyway, so they never get it. */
export function GrantedAccessBanner({ resourceType, resourceId }: { resourceType: AccessResourceType; resourceId: number }) {
  const { isExecutive } = useAuth()
  const status = useMyGrantStatus(resourceType, resourceId, !isExecutive)

  if (isExecutive || !status.data?.viaGrant) return null

  return (
    <div className={styles.banner} role="status">
      Shared with you by {status.data.sharedByName}. This task or incident is not from your department. Any action
      performed is recorded.
    </div>
  )
}
