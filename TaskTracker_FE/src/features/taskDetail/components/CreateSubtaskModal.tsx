import { Modal } from '../../../components/ui/Modal'
import { ErrorMessage } from '../../../components/ui/ErrorMessage'
import { useCreateSubtask } from '../hooks/useCreateSubtask'
import { CreateSubtaskForm } from './CreateSubtaskForm'

interface CreateSubtaskModalProps {
  parentTaskId: number
  /** The parent's own team — meaningless (and unused) when isDepartmentImplementation is
   *  true, since that case picks a team org-wide instead of using one fixed team's roster. */
  teamId: number
  /** True only when the parent is a Department-assigned Executive task. */
  isDepartmentImplementation?: boolean
  open: boolean
  onClose: () => void
}

/** Form shell + submit — owns the mutation, CreateSubtaskForm owns only the fields. */
export function CreateSubtaskModal({
  parentTaskId,
  teamId,
  isDepartmentImplementation,
  open,
  onClose,
}: CreateSubtaskModalProps) {
  const createSubtask = useCreateSubtask(parentTaskId)

  function handleSubmit(payload: Parameters<typeof createSubtask.mutate>[0]) {
    createSubtask.mutate(payload, { onSuccess: onClose })
  }

  return (
    <Modal open={open} onClose={onClose} title={isDepartmentImplementation ? 'New implementation task' : 'New subtask'}>
      {createSubtask.isError && <ErrorMessage message={createSubtask.error.message} />}
      <CreateSubtaskForm
        teamId={teamId}
        isDepartmentImplementation={isDepartmentImplementation}
        onSubmit={handleSubmit}
        onCancel={onClose}
        submitting={createSubtask.isPending}
      />
    </Modal>
  )
}
