import { useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { Modal } from '../../../components/ui/Modal'
import { ErrorMessage } from '../../../components/ui/ErrorMessage'
import { SuccessMessage } from '../../../components/ui/SuccessMessage'
import { useCreatePerson } from '../hooks/useCreatePerson'
import { CreatePersonForm } from './CreatePersonForm'
import type { Person } from '../../../types/person.types'
import styles from '../../teams/components/CreateTeamForm.module.css'

interface CreatePersonModalProps {
  open: boolean
  onClose: () => void
}

/** Form shell + submit — owns the mutation, CreatePersonForm owns only the fields. Stays
 *  open on success (rather than closing straight away) to actually show the confirmation,
 *  and as a reminder that the password just set is the only place it exists — there's no
 *  sign-up step or email to fall back on, so it has to be handed to the person directly. */
export function CreatePersonModal({ open, onClose }: CreatePersonModalProps) {
  const createPerson = useCreatePerson()
  const [created, setCreated] = useState<Person | null>(null)

  function handleSubmit(payload: Parameters<typeof createPerson.mutate>[0]) {
    createPerson.mutate(payload, { onSuccess: setCreated })
  }

  function handleClose() {
    setCreated(null)
    createPerson.reset()
    onClose()
  }

  return (
    <Modal open={open} onClose={handleClose} title="New person">
      {created ? (
        <>
          <SuccessMessage
            message={`${created.fullName} was created and can log in now at ${created.email} with the password you set — share it with them directly.`}
          />
          <div className={styles.actions}>
            <Button onClick={handleClose}>Done</Button>
          </div>
        </>
      ) : (
        <>
          {createPerson.isError && <ErrorMessage message={createPerson.error.message} />}
          <CreatePersonForm onSubmit={handleSubmit} onCancel={handleClose} submitting={createPerson.isPending} />
        </>
      )}
    </Modal>
  )
}
