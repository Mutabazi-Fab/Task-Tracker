import { useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { ErrorMessage } from '../../../components/ui/ErrorMessage'
import { Modal } from '../../../components/ui/Modal'
import { TextField } from '../../../components/ui/TextField'
import { useAuth } from '../../auth/useAuth'
import { useRemoveTeamMember } from '../hooks/useRemoveTeamMember'
import styles from './CreateTeamForm.module.css'

interface RemoveMemberModalProps {
  teamId: number
  personId: number
  personName: string
  open: boolean
  onClose: () => void
}

/** Every remove requires a reason too, same as adding. Note: the backend doesn't yet
 *  block removing a member with unfinished subtasks assigned to them within this team —
 *  a tracked, known gap on the backend, not something this form works around. */
export function RemoveMemberModal({ teamId, personId, personName, open, onClose }: RemoveMemberModalProps) {
  const { currentUser } = useAuth()
  const removeMember = useRemoveTeamMember(teamId)
  const [reason, setReason] = useState('')

  const isValid = reason.trim() !== '' && currentUser !== null

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!isValid || !currentUser) return

    removeMember.mutate(
      { personId, changedById: currentUser.id, reason: reason.trim() },
      {
        onSuccess: () => {
          setReason('')
          onClose()
        },
      },
    )
  }

  return (
    <Modal open={open} onClose={onClose} title={`Remove ${personName}`}>
      {removeMember.isError && <ErrorMessage message={removeMember.error.message} />}
      <form className={styles.form} onSubmit={handleSubmit}>
        <TextField
          label="Reason"
          value={reason}
          onChange={setReason}
          placeholder="Why this person is leaving the team"
          required
        />
        <div className={styles.actions}>
          <Button type="button" variant="ghost" onClick={onClose} disabled={removeMember.isPending}>
            Cancel
          </Button>
          <Button type="submit" variant="primary" disabled={!isValid || removeMember.isPending}>
            {removeMember.isPending ? 'Removing…' : 'Remove member'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
