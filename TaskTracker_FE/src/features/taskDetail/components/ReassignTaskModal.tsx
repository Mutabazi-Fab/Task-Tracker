import { useState } from 'react'
import { Modal } from '../../../components/ui/Modal'
import { SelectField } from '../../../components/ui/SelectField'
import { TextField } from '../../../components/ui/TextField'
import { Button } from '../../../components/ui/Button'
import { ErrorMessage } from '../../../components/ui/ErrorMessage'
import { useAuth } from '../../auth/useAuth'
import { usePeople } from '../../people/hooks/usePeople'
import { useTeams } from '../../teams/hooks/useTeams'
import { useTeamMembers } from '../../teams/hooks/useTeamMembers'
import { useReassignTask } from '../hooks/useReassignTask'
import { useTaskDetail } from '../hooks/useTaskDetail'
import type { TaskDetail } from '../../../types/task.types'
import styles from './ReassignTaskModal.module.css'

interface ReassignTaskModalProps {
  task: TaskDetail
  open: boolean
  onClose: () => void
}

/**
 * Reassignment requires a reason and can never target the current owner. Which kind of
 * target applies is structural, not a free choice, so there's no "assignee type" picker
 * here anymore: a team-assigned task (always top-level) can only move to a different TEAM;
 * an individually-assigned one can only move to a different PERSON — scoped to the parent
 * task's team if this is a subtask, or the whole org if this is a top-level task assigned
 * straight to one person (see CreateTaskForm). "Reassigned by" is always the logged-in
 * person, not a picker — the backend takes it as an explicit field, but there's no reason
 * to ask when we already know who's here.
 */
export function ReassignTaskModal({ task, open, onClose }: ReassignTaskModalProps) {
  const isTeamAssigned = task.assigneeType === 'TEAM'
  const isSubtask = task.parentTaskId !== null

  const [newTeamId, setNewTeamId] = useState('')
  const [newPersonId, setNewPersonId] = useState('')
  const [reason, setReason] = useState('')

  const { currentUser } = useAuth()
  const teamsQuery = useTeams()
  const reassign = useReassignTask(task.id)

  // A subtask's owning team isn't on TaskDetail directly, so its parent (always a
  // team-assigned top-level task) is fetched to read that team's id off the parent's
  // assigneeId. NaN when not applicable keeps both queries disabled. A top-level task
  // assigned straight to one person has no team to scope to at all, so it picks a new
  // owner from the whole org instead.
  const parentQuery = useTaskDetail(isSubtask ? task.parentTaskId ?? NaN : NaN)
  const parentTeamId = parentQuery.data?.assigneeId ?? NaN
  const membersQuery = useTeamMembers(isSubtask ? parentTeamId : NaN)
  const peopleQuery = usePeople(!isTeamAssigned && !isSubtask)

  const personOptions = isSubtask
    ? (membersQuery.data ?? []).map((member) => ({ id: member.personId, name: member.fullName }))
    : (peopleQuery.data ?? []).map((person) => ({ id: person.id, name: person.fullName }))
  const personOptionsLoading = isSubtask ? membersQuery.isLoading : peopleQuery.isLoading

  const isSameOwner = isTeamAssigned
    ? newTeamId !== '' && Number(newTeamId) === task.assigneeId
    : newPersonId !== '' && Number(newPersonId) === task.assigneeId

  const hasTarget = isTeamAssigned ? newTeamId !== '' : newPersonId !== ''
  const isValid = hasTarget && !isSameOwner && reason.trim() !== '' && currentUser !== null

  function handleClose() {
    setNewTeamId('')
    setNewPersonId('')
    setReason('')
    onClose()
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!isValid || !currentUser) return

    reassign.mutate(
      {
        newTeamId: isTeamAssigned ? Number(newTeamId) : undefined,
        newPersonId: isTeamAssigned ? undefined : Number(newPersonId),
        reassignedById: currentUser.id,
        reason: reason.trim(),
      },
      { onSuccess: handleClose },
    )
  }

  return (
    <Modal open={open} onClose={handleClose} title="Reassign task">
      <form className={styles.form} onSubmit={handleSubmit}>
        {isTeamAssigned ? (
          <SelectField
            label="New team"
            value={newTeamId}
            onChange={setNewTeamId}
            placeholder={teamsQuery.isLoading ? 'Loading…' : 'Select a team'}
            options={(teamsQuery.data ?? [])
              .filter((team) => team.id !== task.assigneeId)
              .map((team) => ({ label: team.name, value: String(team.id) }))}
          />
        ) : (
          <SelectField
            label="New owner"
            value={newPersonId}
            onChange={setNewPersonId}
            placeholder={personOptionsLoading ? 'Loading…' : 'Select a person'}
            options={personOptions
              .filter((person) => person.id !== task.assigneeId)
              .map((person) => ({ label: person.name, value: String(person.id) }))}
          />
        )}

        {isSameOwner && <ErrorMessage message="This task is already assigned there." />}

        {/* Read-only, not a picker — the backend still takes reassignedById as an explicit
            field, but there's no reason to ask when we already know who's logged in. Shown
            anyway so a Director or Team Leader can see up front that this reassignment
            will be recorded against their name in the audit trail, not submitted blind. */}
        <div className={styles.field}>
          <span className={styles.label}>Reassigned by</span>
          <span className={styles.value}>{currentUser?.fullName}</span>
          <span className={styles.hint}>This action will be recorded under your name.</span>
        </div>

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
