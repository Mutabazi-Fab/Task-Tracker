import { useState } from 'react'
import { Modal } from '../../../components/ui/Modal'
import { Button } from '../../../components/ui/Button'
import { ErrorMessage } from '../../../components/ui/ErrorMessage'
import { useCreateIncident } from '../hooks/useCreateIncident'
import { CreateIncidentForm } from './CreateIncidentForm'
import { businessUnitLabel, categoryLabel } from '../lib/incidentLabels'
import type { CreateIncidentRequest } from '../../../types/incident.types'
import styles from './CreateIncidentModal.module.css'

interface CreateIncidentModalProps {
  open: boolean
  onClose: () => void
}

/** The form only stages a payload — nothing is sent to the backend until the user explicitly confirms
 *  "Yes, record it" on the summary step that follows. */
export function CreateIncidentModal({ open, onClose }: CreateIncidentModalProps) {
  const createIncident = useCreateIncident()
  const [pendingPayload, setPendingPayload] = useState<CreateIncidentRequest | null>(null)

  function handleClose() {
    setPendingPayload(null)
    onClose()
  }

  async function handleConfirm() {
    if (!pendingPayload) return
    await createIncident.mutateAsync(pendingPayload)
    setPendingPayload(null)
    onClose()
  }

  return (
    <Modal open={open} onClose={handleClose} title={pendingPayload ? 'Confirm incident report' : 'Report incident'} size="lg">
      {createIncident.isError && <ErrorMessage message={createIncident.error.message} />}

      {pendingPayload ? (
        <>
          <p className={styles.confirmText}>Do you want to record this incident?</p>
          <div className={styles.confirmSummary}>
            <div className={styles.confirmRow}>
              <span className={styles.confirmLabel}>Title</span>
              <span>{pendingPayload.title}</span>
            </div>
            <div className={styles.confirmRow}>
              <span className={styles.confirmLabel}>Business unit</span>
              <span>{businessUnitLabel(pendingPayload.businessUnit)}</span>
            </div>
            <div className={styles.confirmRow}>
              <span className={styles.confirmLabel}>Category</span>
              <span>{categoryLabel(pendingPayload.category)}</span>
            </div>
            <div className={styles.confirmRow}>
              <span className={styles.confirmLabel}>Date occurred</span>
              <span>{pendingPayload.dateOccurred}</span>
            </div>
          </div>
          <div className={styles.confirmActions}>
            <Button type="button" variant="ghost" onClick={() => setPendingPayload(null)} disabled={createIncident.isPending}>
              Back to edit
            </Button>
            <Button type="button" variant="primary" onClick={handleConfirm} disabled={createIncident.isPending}>
              {createIncident.isPending ? 'Recording…' : 'Yes, record it'}
            </Button>
          </div>
        </>
      ) : (
        <CreateIncidentForm onSubmit={setPendingPayload} onCancel={handleClose} submitting={createIncident.isPending} />
      )}
    </Modal>
  )
}
