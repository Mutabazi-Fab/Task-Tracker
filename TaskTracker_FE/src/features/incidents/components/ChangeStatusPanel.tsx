import { useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { ErrorMessage } from '../../../components/ui/ErrorMessage'
import { SelectField } from '../../../components/ui/SelectField'
import { TextField } from '../../../components/ui/TextField'
import { useAuth } from '../../auth/useAuth'
import { useChangeIncidentStatus } from '../hooks/useChangeIncidentStatus'
import { statusLabel } from '../lib/incidentLabels'
import type { IncidentDetail, IncidentStatus } from '../../../types/incident.types'
import styles from './ChangeStatusPanel.module.css'

const STATUS_OPTIONS: IncidentStatus[] = ['OPEN', 'UNDER_INVESTIGATION', 'MONITORING', 'CLOSED', 'REJECTED_NOT_AN_INCIDENT']

/** Moves an incident to a new status. Attempting CLOSED while closureReady is false is still
 *  sent (the backend is the real enforcement — see IncidentServiceImpl.requireClosureReadiness);
 *  this panel just surfaces the same blockers up front so the request isn't a surprise. */
export function ChangeStatusPanel({ incident }: { incident: IncidentDetail }) {
  const { currentUser } = useAuth()
  const changeStatus = useChangeIncidentStatus(incident.id)
  const [newStatus, setNewStatus] = useState<IncidentStatus>(incident.status)
  const [note, setNote] = useState('')

  const attemptingClose = newStatus === 'CLOSED' && !incident.closureReady
  const unchanged = newStatus === incident.status

  function handleChange() {
    changeStatus.mutate(
      { newStatus, note: note.trim() || undefined, changedById: currentUser?.id ?? 0 },
      { onSuccess: () => setNote('') },
    )
  }

  return (
    <div className={styles.panel}>
      {changeStatus.isError && <ErrorMessage message={changeStatus.error.message} />}
      {attemptingClose && incident.closureBlockers.length > 0 && (
        <div className={styles.blockers}>
          <strong>Cannot close yet:</strong>
          <ul>
            {incident.closureBlockers.map((b) => (
              <li key={b}>{b}</li>
            ))}
          </ul>
        </div>
      )}
      <div className={styles.row}>
        <div className={styles.field}>
          <SelectField
            label="New status"
            value={newStatus}
            onChange={(v) => setNewStatus(v as IncidentStatus)}
            options={STATUS_OPTIONS.map((s) => ({ label: statusLabel(s), value: s }))}
          />
        </div>
        <div className={styles.field}>
          <TextField label="Note (optional)" value={note} onChange={setNote} placeholder="Why this change" />
        </div>
        <Button
          onClick={handleChange}
          disabled={unchanged || (attemptingClose && incident.closureBlockers.length > 0) || changeStatus.isPending}
        >
          {changeStatus.isPending ? 'Updating…' : 'Update status'}
        </Button>
      </div>
    </div>
  )
}
