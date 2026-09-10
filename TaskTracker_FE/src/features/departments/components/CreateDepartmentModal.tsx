import { Modal } from '../../../components/ui/Modal'
import { ErrorMessage } from '../../../components/ui/ErrorMessage'
import { useCreateDepartment } from '../hooks/useCreateDepartment'
import { CreateDepartmentForm } from './CreateDepartmentForm'

interface CreateDepartmentModalProps {
  open: boolean
  onClose: () => void
}

/** Form shell + submit — owns the mutation, CreateDepartmentForm owns only the fields. */
export function CreateDepartmentModal({ open, onClose }: CreateDepartmentModalProps) {
  const createDepartment = useCreateDepartment()

  function handleSubmit(payload: Parameters<typeof createDepartment.mutate>[0]) {
    createDepartment.mutate(payload, { onSuccess: onClose })
  }

  return (
    <Modal open={open} onClose={onClose} title="New department">
      {createDepartment.isError && <ErrorMessage message={createDepartment.error.message} />}
      <CreateDepartmentForm onSubmit={handleSubmit} onCancel={onClose} submitting={createDepartment.isPending} />
    </Modal>
  )
}
