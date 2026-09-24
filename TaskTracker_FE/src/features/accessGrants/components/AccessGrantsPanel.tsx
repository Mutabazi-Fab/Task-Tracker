import { useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { EmptyState } from '../../../components/ui/EmptyState'
import { ErrorMessage } from '../../../components/ui/ErrorMessage'
import { formatDate } from '../../../lib/formatDate'
import { useAuth } from '../../auth/useAuth'
import { useCanManageGrants, useGrantsForResource, useRevokeAccess } from '../hooks/useAccessGrants'
import { ShareAccessModal } from './ShareAccessModal'
import type { AccessResourceType } from '../../../types/accessGrant.types'
import styles from './AccessGrants.module.css'

/** Who has been given access to this one item, with a button to share it with someone else and one to remove
 *  each person's access. Shown to an Executive/Super Admin, and to a Director on an item from their own
 *  department; nothing for anyone else. */
export function AccessGrantsPanel({ resourceType, resourceId }: { resourceType: AccessResourceType; resourceId: number }) {
  const { isExecutive, isDirector } = useAuth()
  const canManage = useCanManageGrants(resourceType, resourceId, isDirector)
  const allowed = isExecutive || canManage.data === true
  const grants = useGrantsForResource(resourceType, resourceId, allowed)
  const revoke = useRevokeAccess()
  const [shareOpen, setShareOpen] = useState(false)

  if (!allowed) return null

  return (
    <Card>
      <div className={styles.row}>
        <span className={styles.name}>People with special access</span>
        <Button variant="secondary" onClick={() => setShareOpen(true)}>
          Share access
        </Button>
      </div>
      {revoke.isError && <ErrorMessage message={revoke.error.message} />}
      {grants.data && grants.data.length === 0 ? (
        <EmptyState title="Not shared with anyone" description="Only people who normally see this can open it." />
      ) : (
        <div className={styles.list}>
          {(grants.data ?? []).map((grant) => (
            <div key={grant.id} className={styles.row}>
              <div className={styles.who}>
                <span className={styles.name}>
                  {grant.granteeName} <span className={styles.meta}>— {grant.granteeJobTitle}</span>
                </span>
                <span className={styles.meta}>
                  Shared by {grant.grantedByName} on {formatDate(grant.grantedAt)} · {grant.reason}
                </span>
              </div>
              <Button variant="danger" onClick={() => revoke.mutate(grant.id)} disabled={revoke.isPending}>
                Remove access
              </Button>
            </div>
          ))}
        </div>
      )}
      <ShareAccessModal open={shareOpen} onClose={() => setShareOpen(false)} resourceType={resourceType} resourceId={resourceId} />
    </Card>
  )
}
