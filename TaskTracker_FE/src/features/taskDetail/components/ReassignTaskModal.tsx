import { useState } from 'react'
import { Modal } from '../../../components/ui/Modal'
import { SegmentedControl } from '../../../components/ui/SegmentedControl'
import { SelectField } from '../../../components/ui/SelectField'
import { TextField } from '../../../components/ui/TextField'
import { Button } from '../../../components/ui/Button'
import { ErrorMessage } from '../../../components/ui/ErrorMessage'
import { useReassignTask } from '../hooks/useReassignTask'
import { usePeople } from '../../people/hooks/usePeople'
import { useTeams } from '../../teams/hooks/useTeams'
import type { AssigneeType, TaskDetail } from '../../../types/task.types'
import styles from './ReassignTaskModal.module.css'

interface ReassignTaskModalProps {
  task: TaskDetail
  open: boolean
  onClose: () => void
}

const ASSIGNEE_TYPE_OPTIONS: { label: string; value: AssigneeType }[] = [
  { label: 'Individual', value: 'INDIVIDUAL' },
  { label: 'Team', value: 'TEAM' },
]

/** Reassignment requires a reason and can never target the current owner. */
export function ReassignTaskModal({ task, open, onClose }: ReassignTaskModalProps) {
  const [newAssigneeType, setNewAssigneeType] = useState<AssigneeType>(task.assigneeType)
  const [newPersonId, setNewPersonId] = useState('')
  const [newTeamId, setNewTeamId] = useState('')
  const [reassignedById, setReassignedById] = useState('')
  const [reason, setReason] = useState('')

  const peopleQuery = usePeople()
  const teamQuery = useTeams()
  const reassign = useReassignTask(task.id)

  const isSameOwner =
    newAssigneeType === task.assigneeType &&
    (newAssigneeType === 'INDIVIDUAL'
      ? newPersonId !== '' && Number(newPersonId) === task.assigneeId
      : newTeamId !== '' && Number(newTeamId) === task.assigneeId)

  const hasTarget = newAssigneeType === 'INDIVIDUAL' ? newPersonId !== '' : newTeamId !== ''
  const isValid = hasTarget && !isSameOwner && reassignedById !== '' && reason.trim() !== ''

  function handleClose() {
    setNewPersonId('')
    setNewTeamId('')
    setReassignedById('')
    setReason('')
    onClose()
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!isValid) return

    reassign.mutate(
      {
        newAssigneeType,
        newPersonId: newAssigneeType === 'INDIVIDUAL' ? Number(newPersonId) : undefined,
        newTeamId: newAssigneeType === 'TEAM' ? Number(newTeamId) : undefined,
        reassignedById: Number(reassignedById),
        reason: reason.trim(),
      },
      { onSuccess: handleClose },
    )
  }

  return (
    <Modal open={open} onClose={handleClose} title="Reassign task">
      <form className={styles.form} onSubmit={handleSubmit}>
        <div className={styles.field}>
          <span className={styles.label}>New assignee type</span>
          <SegmentedControl options={ASSIGNEE_TYPE_OPTIONS} value={newAssigneeType} onChange={setNewAssigneeType} />
        </div>

        {newAssigneeType === 'INDIVIDUAL' ? (
          <SelectField
            label="New owner"
            value={newPersonId}
            onChange={setNewPersonId}
            placeholder={peopleQuery.isLoading ? 'Loading…' : 'Select a person'}
            options={(peopleQuery.data ?? []).map((person) => ({ label: person.fullName, value: String(person.id) }))}
          />
        ) : (
          <SelectField
            label="New owner"
            value={newTeamId}
            onChange={setNewTeamId}
            placeholder={teamQuery.isLoading ? 'Loading…' : 'Select a team'}
            options={(teamQuery.data ?? []).map((team) => ({ label: team.name, value: String(team.id) }))}
          />
        )}

        {isSameOwner && <ErrorMessage message="This task is already assigned there." />}

        <SelectField
          label="Reassigned by"
          value={reassignedById}
          onChange={setReassignedById}
          placeholder={peopleQuery.isLoading ? 'Loading…' : 'Select a person'}
          options={(peopleQuery.data ?? []).map((person) => ({ label: person.fullName, value: String(person.id) }))}
        />

        <TextField label="Reason" value={reason} onChange={setReason} placeholder="Why this task is moving" required />

        {reassign.isError && <ErrorMessage message={reassign.error.message} />}

        <div className={styles.actions}>
          <Button type="button" variant="ghost" onClick={handleClose} disabled={reassign.isPending}>
            Cancel
          </Button>
          <Button type="submit" disabled={!isValid || reassign.isPending}>
            {reassign.isPending ? 'Reassigning…' : 'Reassign'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
