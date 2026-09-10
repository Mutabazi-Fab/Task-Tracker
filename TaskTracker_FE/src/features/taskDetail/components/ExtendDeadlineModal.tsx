import { useState } from 'react'
import { Modal } from '../../../components/ui/Modal'
import { TextField } from '../../../components/ui/TextField'
import { Button } from '../../../components/ui/Button'
import { ErrorMessage } from '../../../components/ui/ErrorMessage'
import { useAuth } from '../../auth/useAuth'
import { useExtendDeadlineDirectly } from '../hooks/useExtendDeadlineDirectly'
import type { TaskDetail } from '../../../types/task.types'
import styles from './ReassignTaskModal.module.css'

interface ExtendDeadlineModalProps {
  task: TaskDetail
  open: boolean
  onClose: () => void
}

/** A direct extension, no approval round-trip — only whoever set this task's deadline (or
 *  a Director/Super Admin override, Executive/Super Admin for a Department task) sees this
 *  button at all (see TaskDetailPage). Still logged in full in the deadline history below,
 *  self-approved, so the audit trail has no gap just because the shortcut path was used. */
export function ExtendDeadlineModal({ task, open, onClose }: ExtendDeadlineModalProps) {
  const [newDeadline, setNewDeadline] = useState('')
  const [reason, setReason] = useState('')

  const { currentUser } = useAuth()
  const extendDeadline = useExtendDeadlineDirectly(task.id)

  const isPastCurrentDeadline = task.deadline !== null && newDeadline !== '' && newDeadline <= task.deadline
  const isValid = newDeadline !== '' && !isPastCurrentDeadline && currentUser !== null

  function handleClose() {
    setNewDeadline('')
    setReason('')
    onClose()
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!isValid || !currentUser) return

    extendDeadline.mutate(
      { newDeadline, reason: reason.trim() || undefined, extendedById: currentUser.id },
      { onSuccess: handleClose },
    )
  }

  return (
    <Modal open={open} onClose={handleClose} title="Extend deadline">
      <form className={styles.form} onSubmit={handleSubmit}>
        <TextField
          label="New deadline"
          type="date"
          value={newDeadline}
          onChange={setNewDeadline}
          min={task.deadline ?? undefined}
          required
        />
        {isPastCurrentDeadline && <ErrorMessage message="The new deadline must be after the current one." />}

        <TextField label="Reason (optional)" value={reason} onChange={setReason} placeholder="Why this is moving" />

        {extendDeadline.isError && <ErrorMessage message={extendDeadline.error.message} />}

        <div className={styles.actions}>
          <Button type="button" variant="ghost" onClick={handleClose} disabled={extendDeadline.isPending}>
            Cancel
          </Button>
          <Button type="submit" disabled={!isValid || extendDeadline.isPending}>
            {extendDeadline.isPending ? 'Extending…' : 'Extend deadline'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
