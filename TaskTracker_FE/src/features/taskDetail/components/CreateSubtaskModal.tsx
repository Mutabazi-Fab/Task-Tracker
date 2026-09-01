import { Modal } from '../../../components/ui/Modal'
import { ErrorMessage } from '../../../components/ui/ErrorMessage'
import { useCreateSubtask } from '../hooks/useCreateSubtask'
import { CreateSubtaskForm } from './CreateSubtaskForm'

interface CreateSubtaskModalProps {
  parentTaskId: number
  teamId: number
  open: boolean
  onClose: () => void
}

/** Form shell + submit — owns the mutation, CreateSubtaskForm owns only the fields. */
export function CreateSubtaskModal({ parentTaskId, teamId, open, onClose }: CreateSubtaskModalProps) {
  const createSubtask = useCreateSubtask(parentTaskId)

  function handleSubmit(payload: Parameters<typeof createSubtask.mutate>[0]) {
    createSubtask.mutate(payload, { onSuccess: onClose })
  }

  return (
    <Modal open={open} onClose={onClose} title="New subtask">
      {createSubtask.isError && <ErrorMessage message={createSubtask.error.message} />}
      <CreateSubtaskForm teamId={teamId} onSubmit={handleSubmit} onCancel={onClose} submitting={createSubtask.isPending} />
    </Modal>
  )
}
