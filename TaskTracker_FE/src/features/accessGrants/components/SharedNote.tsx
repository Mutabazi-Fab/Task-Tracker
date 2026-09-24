import { useSharedWithMe } from '../hooks/useAccessGrants'
import type { AccessResourceType } from '../../../types/accessGrant.types'
import styles from './AccessGrants.module.css'

/** A small line under a task's or incident's title in a list when it isn't originally the viewer's: it was
 *  shared with them, and by whom. Renders nothing for an item that is theirs in the ordinary way. */
export function SharedNote({ resourceType, resourceId }: { resourceType: Exclude<AccessResourceType, 'RISK'>; resourceId: number }) {
  const shared = useSharedWithMe()
  const grant = (shared.data ?? []).find((g) => g.resourceType === resourceType && g.resourceId === resourceId)

  if (!grant) return null

  return (
    <span className={styles.sharedNote}>
      Not originally yours — shared with you by {grant.grantedByName}
    </span>
  )
}
