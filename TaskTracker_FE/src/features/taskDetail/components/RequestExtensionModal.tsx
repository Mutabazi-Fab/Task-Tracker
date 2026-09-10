import { useState } from 'react'
import { Modal } from '../../../components/ui/Modal'
import { TextField } from '../../../components/ui/TextField'
import { Button } from '../../../components/ui/Button'
import { ErrorMessage } from '../../../components/ui/ErrorMessage'
import { useAuth } from '../../auth/useAuth'
import { useRequestDeadlineExtension } from '../hooks/useRequestDeadlineExtension'
import type { TaskDetail } from '../../../types/task.types'
import styles from './ReassignTaskModal.module.css'

interface RequestExtensionModalProps {
  task: TaskDetail
  open: boolean
  onClose: () => void
}

/** Requested by whoever's actually doing the work (this task's Team Leader, individual
 *  assignee, or — for a Department task — its head Director), sent up the chain of
 *  command to this task's deadline decider (task.deadlineDeciderName) — always a Director-
 *  or-above, even when a Team Leader technically created this task as a leaf subtask (see
 *  TaskServiceImpl.resolveDeadlineDecider). Never moves the deadline itself — only an
 *  approval does that (see DeadlineExtensionHistoryItem's inline approve/reject). */
export function RequestExtensionModal({ task, open, onClose }: RequestExtensionModalProps) {
  const [requestedDeadline, setRequestedDeadline] = useState('')
  const [justification, setJustification] = useState('')

  const { currentUser } = useAuth()
  const requestExtension = useRequestDeadlineExtension(task.id)

  const isPastCurrentDeadline = task.deadline !== null && requestedDeadline !== '' && requestedDeadline <= task.deadline
  const isValid =
    requestedDeadline !== '' && !isPastCurrentDeadline && justification.trim() !== '' && currentUser !== null

  function handleClose() {
    setRequestedDeadline('')
    setJustification('')
    onClose()
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!isValid || !currentUser) return

    requestExtension.mutate(
      { requestedDeadline, justification: justification.trim(), requestedById: currentUser.id },
      { onSuccess: handleClose },
    )
  }

  return (
    <Modal open={open} onClose={handleClose} title="Request a deadline extension">
      <form className={styles.form} onSubmit={handleSubmit}>
        <p className={styles.hint}>
          Sent to {task.deadlineDeciderName}, this task's deadline decider
          {task.deadline ? ` (current deadline: ${task.deadline})` : ''}, to approve or reject.
        </p>

        <TextField
          label="New deadline"
          type="date"
          value={requestedDeadline}
          onChange={setRequestedDeadline}
          min={task.deadline ?? undefined}
          required
        />
        {isPastCurrentDeadline && <ErrorMessage message="The new deadline must be after the current one." />}

        <TextField
          label="Justification"
          value={justification}
          onChange={setJustification}
          placeholder="Why more time is needed"
          required
        />

        {requestExtension.isError && <ErrorMessage message={requestExtension.error.message} />}

        <div className={styles.actions}>
          <Button type="button" variant="ghost" onClick={handleClose} disabled={requestExtension.isPending}>
            Cancel
          </Button>
          <Button type="submit" disabled={!isValid || requestExtension.isPending}>
            {requestExtension.isPending ? 'Sending…' : 'Send request'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
