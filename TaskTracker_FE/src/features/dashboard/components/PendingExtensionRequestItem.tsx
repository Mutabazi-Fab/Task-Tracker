import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ROUTES } from '../../../app/routePaths'
import { Button } from '../../../components/ui/Button'
import { ErrorMessage } from '../../../components/ui/ErrorMessage'
import { TextField } from '../../../components/ui/TextField'
import { formatDateTime } from '../../../lib/formatDate'
import { useAuth } from '../../auth/useAuth'
import { useDecideDeadlineExtension } from '../../taskDetail/hooks/useDecideDeadlineExtension'
import { useForwardDeadlineExtension } from '../../taskDetail/hooks/useForwardDeadlineExtension'
import type { PendingExtensionRequest } from '../../../types/deadlineExtension.types'
import styles from './PendingExtensionRequestItem.module.css'

interface PendingExtensionRequestItemProps {
  request: PendingExtensionRequest
}

/** One row of the "Requests" inbox — every row here is guaranteed PENDING and already
 *  scoped to requests the viewer is actually a decider for (the backend does that
 *  filtering — see TaskServiceImpl.getPendingExtensionRequests), so unlike
 *  DeadlineExtensionHistoryItem there's no canDecide check to make here for whether to show
 *  this row at all. But "a decider" isn't always "the full decider": on a task chain that
 *  originated from the CEO's own mandate, the Director it first landed on can still reject
 *  it, but only the CEO/Super Admin can approve it — see request.canApprove, which hides
 *  the Approve action rather than showing a button that would just fail. Before the CEO
 *  has actually seen it, the Director gets a "Send to CEO for approval" action instead (see
 *  request.forwardedToApprover) — the CEO's own inbox doesn't fill up automatically the
 *  moment a request like this is made, only once a Director deliberately escalates it. */
export function PendingExtensionRequestItem({ request }: PendingExtensionRequestItemProps) {
  const [decisionNote, setDecisionNote] = useState('')
  const { currentUser } = useAuth()
  const decide = useDecideDeadlineExtension(request.taskId)
  const forward = useForwardDeadlineExtension(request.taskId)

  function handleDecide(approve: boolean) {
    if (!currentUser) return
    decide.mutate({
      extensionId: request.id,
      approve,
      decisionNote: decisionNote.trim() || undefined,
      decidedById: currentUser.id,
    })
  }

  function handleForward() {
    forward.mutate(request.id)
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
          {request.canApprove && (
            <Button type="button" variant="secondary" onClick={() => handleDecide(true)} disabled={decide.isPending}>
              {decide.isPending ? 'Saving…' : 'Approve'}
            </Button>
          )}
          {!request.canApprove && !request.forwardedToApprover && (
            <Button type="button" variant="secondary" onClick={handleForward} disabled={forward.isPending}>
              {forward.isPending ? 'Sending…' : 'Send to CEO for approval'}
            </Button>
          )}
          <Button type="button" variant="danger" onClick={() => handleDecide(false)} disabled={decide.isPending}>
            {decide.isPending ? 'Saving…' : 'Reject'}
          </Button>
        </div>
        {!request.canApprove && (
          <p className={styles.approvalNote}>
            {request.forwardedToApprover
              ? 'This originated from the CEO’s own mandate and has been sent to the CEO for approval — you can still reject it.'
              : 'This originated from the CEO’s own mandate — only the CEO or Super Admin can approve it. Send it to the CEO, or reject it yourself.'}
          </p>
        )}
        {decide.isError && <ErrorMessage message={decide.error.message} />}
        {forward.isError && <ErrorMessage message={forward.error.message} />}
      </div>
    </div>
  )
}
