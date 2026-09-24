import { Modal } from '../../../components/ui/Modal'
import { ErrorMessage } from '../../../components/ui/ErrorMessage'
import { useAuth } from '../../auth/useAuth'
import { useUpdateIncident } from '../hooks/useUpdateIncident'
import { EditIncidentForm } from './EditIncidentForm'
import type { IncidentDetail, UpdateIncidentRequest } from '../../../types/incident.types'

interface EditIncidentModalProps {
  incident: IncidentDetail
  open: boolean
  onClose: () => void
}

export function EditIncidentModal({ incident, open, onClose }: EditIncidentModalProps) {
  const { currentUser } = useAuth()
  const updateIncident = useUpdateIncident(incident.id)

  async function handleSubmit(payload: Omit<UpdateIncidentRequest, 'changedById'>) {
    // changedById is overwritten server-side from the JWT regardless — see IncidentController.updateIncident.
    await updateIncident.mutateAsync({ ...payload, changedById: currentUser?.id ?? 0 })
    onClose()
  }

  return (
    <Modal open={open} onClose={onClose} title={`Edit ${incident.incidentCode}`} size="lg">
      {updateIncident.isError && <ErrorMessage message={updateIncident.error.message} />}
      <EditIncidentForm incident={incident} onSubmit={handleSubmit} onCancel={onClose} submitting={updateIncident.isPending} />
    </Modal>
  )
}
