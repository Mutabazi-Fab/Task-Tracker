import { Modal } from '../../../components/ui/Modal'
import { ErrorMessage } from '../../../components/ui/ErrorMessage'
import { useAddTeamMember } from '../hooks/useAddTeamMember'
import type { TeamMember } from '../../../types/team.types'
import { AddMemberForm } from './AddMemberForm'

interface AddMemberModalProps {
  teamId: number
  existingMembers: TeamMember[]
  open: boolean
  onClose: () => void
}

export function AddMemberModal({ teamId, existingMembers, open, onClose }: AddMemberModalProps) {
  const addMember = useAddTeamMember(teamId)

  function handleSubmit(payload: Parameters<typeof addMember.mutate>[0]) {
    addMember.mutate(payload, { onSuccess: onClose })
  }

  return (
    <Modal open={open} onClose={onClose} title="Add member">
      {addMember.isError && <ErrorMessage message={addMember.error.message} />}
      <AddMemberForm
        existingMembers={existingMembers}
        onSubmit={handleSubmit}
        onCancel={onClose}
        submitting={addMember.isPending}
      />
    </Modal>
  )
}
