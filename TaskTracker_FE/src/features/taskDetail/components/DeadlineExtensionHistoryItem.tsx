import { useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { ErrorMessage } from '../../../components/ui/ErrorMessage'
import { TextField } from '../../../components/ui/TextField'
import { formatDateTime } from '../../../lib/formatDate'
import { useAuth } from '../../auth/useAuth'
import { useDecideDeadlineExtension } from '../hooks/useDecideDeadlineExtension'
import type { DeadlineExtension, ExtensionRequestStatus } from '../../../types/deadlineExtension.types'
import statusChipStyles from '../../../components/ui/StatusChip.module.css'
import styles from './DeadlineExtensionHistoryItem.module.css'

const STATUS_CLASS: Record<ExtensionRequestStatus, string> = {
  PENDING: statusChipStyles.ongoing,
  APPROVED: statusChipStyles.completed,
  REJECTED: statusChipStyles.pending,
}

interface DeadlineExtensionHistoryItemProps {
  extension: DeadlineExtension
  taskId: number
  /** True only for whoever can decide THIS task's extensions (its setter, or the Director/Super Admin —
   *  Executive/Super Admin for a Department task — override) — see TaskDetailPage. */
  canDecide: boolean
}

/** One request, and however it was (or wasn't yet) decided. */
export function DeadlineExtensionHistoryItem({ extension, taskId, canDecide }: DeadlineExtensionHistoryItemProps) {
  const [decisionNote, setDecisionNote] = useState('')
  const { currentUser } = useAuth()
  const decide = useDecideDeadlineExtension(taskId)

  const canAct = canDecide && extension.status === 'PENDING'

  function handleDecide(approve: boolean) {
    if (!currentUser) return
    decide.mutate({
      extensionId: extension.id,
      approve,
      decisionNote: decisionNote.trim() || undefined,
      decidedById: currentUser.id,
    })
  }

  return (
    <div className={styles.item}>
      <div className={styles.transfer}>
        <span className={styles.name}>
          {extension.currentDeadline ?? 'no deadline'} → {extension.requestedDeadline}
        </span>
        <span className={[statusChipStyles.chip, STATUS_CLASS[extension.status]].join(' ')}>{extension.status}</span>
      </div>
      <p className={styles.reason}>{extension.justification}</p>
      <p className={styles.meta}>
        Requested by {extension.requestedByName} · {formatDateTime(extension.requestedAt)}
      </p>
      {extension.status !== 'PENDING' && (
        <p className={styles.meta}>
          {extension.status === 'APPROVED' ? 'Approved' : 'Rejected'} by {extension.decidedByName} ·{' '}
          {extension.decidedAt && formatDateTime(extension.decidedAt)}
          {extension.decisionNote ? `: ${extension.decisionNote}` : ''}
        </p>
      )}

      {canAct && (
        <div className={styles.decideRow}>
          <TextField
            label="Decision note (optional)"
            value={decisionNote}
            onChange={setDecisionNote}
            placeholder="Why you're approving or rejecting"
          />
          <div className={styles.decideActions}>
            <Button type="button" variant="secondary" onClick={() => handleDecide(true)} disabled={decide.isPending}>
              {decide.isPending ? 'Saving…' : 'Approve'}
            </Button>
            <Button type="button" variant="danger" onClick={() => handleDecide(false)} disabled={decide.isPending}>
              {decide.isPending ? 'Saving…' : 'Reject'}
            </Button>
          </div>
          {decide.isError && <ErrorMessage message={decide.error.message} />}
        </div>
      )}
    </div>
  )
}
