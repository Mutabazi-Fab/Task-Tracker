import { useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { ErrorMessage } from '../../../components/ui/ErrorMessage'
import { Modal } from '../../../components/ui/Modal'
import { SelectField } from '../../../components/ui/SelectField'
import { TextField } from '../../../components/ui/TextField'
import { useAuth } from '../../auth/useAuth'
import { usePeople } from '../../people/hooks/usePeople'
import { useGrantAccess } from '../hooks/useAccessGrants'
import type { AccessResourceType } from '../../../types/accessGrant.types'
import styles from './AccessGrants.module.css'

interface ShareAccessModalProps {
  open: boolean
  onClose: () => void
  resourceType: AccessResourceType
  resourceId: number
}

/** Executive/Super Admin picks one person and says why. Executives and Super Admins are left out of the
 *  list — they can already see everything. The person gets a notification, and can view and act on this
 *  one item until access is removed. */
export function ShareAccessModal({ open, onClose, resourceType, resourceId }: ShareAccessModalProps) {
  const { currentUser } = useAuth()
  const peopleQuery = usePeople()
  const grant = useGrantAccess()
  const [granteeId, setGranteeId] = useState('')
  const [reason, setReason] = useState('')

  const candidates = (peopleQuery.data ?? []).filter(
    (p) => p.active && p.role !== 'EXECUTIVE' && p.role !== 'SUPER_ADMIN',
  )
  const isValid = granteeId !== '' && reason.trim() !== '' && currentUser !== null

  function close() {
    setGranteeId('')
    setReason('')
    grant.reset()
    onClose()
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    if (!isValid || !currentUser) return
    await grant.mutateAsync({
      resourceType,
      resourceId,
      granteeId: Number(granteeId),
      reason: reason.trim(),
      grantedById: currentUser.id,
    })
    close()
  }

  return (
    <Modal open={open} onClose={close} title="Share access">
      <form className={styles.form} onSubmit={submit}>
        <p className={styles.meta}>
          This person will be able to view this item and act on it, even though it isn't from their department.
          Every action is recorded. Access lasts until you remove it.
        </p>
        {grant.isError && <ErrorMessage message={grant.error.message} />}
        <SelectField
          label="Person"
          value={granteeId}
          onChange={setGranteeId}
          placeholder={peopleQuery.isLoading ? 'Loading…' : 'Select a person'}
          options={candidates.map((p) => ({ label: `${p.fullName} — ${p.jobTitle}`, value: String(p.id) }))}
        />
        <TextField label="Reason" value={reason} onChange={setReason} placeholder="Why they need access" required />
        <div className={styles.actions}>
          <Button type="button" variant="ghost" onClick={close} disabled={grant.isPending}>
            Cancel
          </Button>
          <Button type="submit" disabled={!isValid || grant.isPending}>
            {grant.isPending ? 'Sharing…' : 'Share access'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
