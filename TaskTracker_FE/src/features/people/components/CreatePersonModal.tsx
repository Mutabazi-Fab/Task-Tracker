import { Modal } from '../../../components/ui/Modal'
import { ErrorMessage } from '../../../components/ui/ErrorMessage'
import { useCreatePerson } from '../hooks/useCreatePerson'
import { CreatePersonForm } from './CreatePersonForm'

interface CreatePersonModalProps {
  open: boolean
  onClose: () => void
}

/** Form shell + submit — owns the mutation, CreatePersonForm owns only the fields. */
export function CreatePersonModal({ open, onClose }: CreatePersonModalProps) {
  const createPerson = useCreatePerson()

  function handleSubmit(payload: Parameters<typeof createPerson.mutate>[0]) {
    createPerson.mutate(payload, { onSuccess: onClose })
  }

  return (
    <Modal open={open} onClose={onClose} title="New person">
      {createPerson.isError && <ErrorMessage message={createPerson.error.message} />}
      <CreatePersonForm onSubmit={handleSubmit} onCancel={onClose} submitting={createPerson.isPending} />
    </Modal>
  )
}
