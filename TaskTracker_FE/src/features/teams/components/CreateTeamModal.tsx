import { Modal } from '../../../components/ui/Modal'
import { ErrorMessage } from '../../../components/ui/ErrorMessage'
import { useCreateTeam } from '../hooks/useCreateTeam'
import { CreateTeamForm } from './CreateTeamForm'

interface CreateTeamModalProps {
  open: boolean
  onClose: () => void
  /** Passed straight through to CreateTeamForm — set when opened from a specific
   *  Department's own page (see DepartmentPage) to lock the department picker. */
  fixedDepartmentId?: number
  fixedDepartmentName?: string
}

/** Form shell + submit — owns the mutation, CreateTeamForm owns only the fields. */
export function CreateTeamModal({ open, onClose, fixedDepartmentId, fixedDepartmentName }: CreateTeamModalProps) {
  const createTeam = useCreateTeam()

  function handleSubmit(payload: Parameters<typeof createTeam.mutate>[0]) {
    createTeam.mutate(payload, { onSuccess: onClose })
  }

  return (
    <Modal open={open} onClose={onClose} title="New team">
      {createTeam.isError && <ErrorMessage message={createTeam.error.message} />}
      <CreateTeamForm
        onSubmit={handleSubmit}
        onCancel={onClose}
        submitting={createTeam.isPending}
        fixedDepartmentId={fixedDepartmentId}
        fixedDepartmentName={fixedDepartmentName}
      />
    </Modal>
  )
}
