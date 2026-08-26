import { Modal } from '../../../components/ui/Modal'
import { ErrorMessage } from '../../../components/ui/ErrorMessage'
import { useCreateTask } from '../hooks/useCreateTask'
import { CreateTaskForm } from './CreateTaskForm'

interface CreateTaskModalProps {
  open: boolean
  onClose: () => void
}

/** Form shell + submit — owns the mutation, CreateTaskForm owns only the fields. */
export function CreateTaskModal({ open, onClose }: CreateTaskModalProps) {
  const createTask = useCreateTask()

  function handleSubmit(payload: Parameters<typeof createTask.mutate>[0]) {
    createTask.mutate(payload, { onSuccess: onClose })
  }

  return (
    <Modal open={open} onClose={onClose} title="New task">
      {createTask.isError && <ErrorMessage message={createTask.error.message} />}
      <CreateTaskForm onSubmit={handleSubmit} onCancel={onClose} submitting={createTask.isPending} />
    </Modal>
  )
}
