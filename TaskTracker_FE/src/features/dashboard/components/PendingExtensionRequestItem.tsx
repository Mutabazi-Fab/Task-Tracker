import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ROUTES } from '../../../app/routePaths'
import { Button } from '../../../components/ui/Button'
import { ErrorMessage } from '../../../components/ui/ErrorMessage'
import { TextField } from '../../../components/ui/TextField'
import { formatDateTime } from '../../../lib/formatDate'
import { useAuth } from '../../auth/useAuth'
import { useDecideDeadlineExtension } from '../../taskDetail/hooks/useDecideDeadlineExtension'
import type { PendingExtensionRequest } from '../../../types/deadlineExtension.types'
import styles from './PendingExtensionRequestItem.module.css'

interface PendingExtensionRequestItemProps {
  request: PendingExtensionRequest
}

/** One row of the "Requests" inbox — every row here is guaranteed PENDING and already
 *  scoped to requests the viewer is actually the decider for (the backend does that
 *  filtering — see TaskServiceImpl.getPendingExtensionRequests), so unlike
 *  DeadlineExtensionHistoryItem there's no canDecide check to make here: if it showed up,
 *  it's this viewer's to decide. */
export function PendingExtensionRequestItem({ request }: PendingExtensionRequestItemProps) {
  const [decisionNote, setDecisionNote] = useState('')
  const { currentUser } = useAuth()
  const decide = useDecideDeadlineExtension(request.taskId)

  function handleDecide(approve: boolean) {
    if (!currentUser) return
    decide.mutate({
      extensionId: request.id,
      approve,
      decisionNote: decisionNote.trim() || undefined,
      decidedById: currentUser.id,
    })
  }

  return (
    <div className={styles.item}>
      <div className={styles.top}>
        <Link to={ROUTES.taskDetail(request.taskId)} className={styles.taskLink}>
          {request.taskCode} · {request.taskTitle}
        </Link>
        <span className={styles.transfer}>
          {request.currentDeadline ?? 'no deadline'} → {request.requestedDeadline}
        </span>
      </div>
      <p className={styles.reason}>{request.justification}</p>
      <p className={styles.meta}>
        Requested by {request.requestedByName} · {formatDateTime(request.requestedAt)}
      </p>
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
    </div>
  )
}
