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
import { useDepartments } from '../../departments/hooks/useDepartments'
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
 * here anymore: a DEPARTMENT-assigned task (always depth 0) can only move to a different
 * DEPARTMENT — e.g. the CEO's office realizes what was handed to IT actually belongs to
 * Cybersecurity — restricted to Executive/Super Admin, since that's the same authority
 * that assigns one in the first place (see TaskDetailPage's canReassign). A team-assigned
 * task can only move to a different TEAM; an individually-assigned one can only move to a
 * different PERSON — scoped to the parent task's team for an ordinary leaf subtask, or the
 * whole org for anything "top-level-shaped": a real top-level task, or a depth-1
 * Department implementation task (see CreateTaskForm/CreateSubtaskForm — mirrors
 * TaskServiceImpl.isTopLevelShaped exactly). "Reassigned by" is always the logged-in
 * person, not a picker — the backend takes it as an explicit field, but there's no reason
 * to ask when we already know who's here.
 */
export function ReassignTaskModal({ task, open, onClose }: ReassignTaskModalProps) {
  const isDepartmentAssigned = task.assigneeType === 'DEPARTMENT'
  const isTeamAssigned = task.assigneeType === 'TEAM'
  const hasParent = task.parentTaskId !== null

  const [newTeamId, setNewTeamId] = useState('')
  const [newPersonId, setNewPersonId] = useState('')
  const [newDepartmentId, setNewDepartmentId] = useState('')
  const [reason, setReason] = useState('')

  const { currentUser } = useAuth()
  const teamsQuery = useTeams()
  const departmentsQuery = useDepartments(isDepartmentAssigned)
  const reassign = useReassignTask(task.id)

  // A leaf subtask's owning team isn't on TaskDetail directly, so its parent is fetched to
  // read that team's id off the parent's assigneeId — but ONLY when the parent is itself
  // TEAM-assigned; a depth-1 Department implementation task's parent is DEPARTMENT-typed
  // (no team of its own), and that case picks a new owner from the whole org instead, same
  // as a real top-level task assigned straight to one person. NaN when not applicable
  // keeps every query below disabled.
  const parentQuery = useTaskDetail(hasParent ? task.parentTaskId ?? NaN : NaN)
  const isRestrictedToParentTeam = hasParent && parentQuery.data?.assigneeType !== 'DEPARTMENT'
  const parentTeamId = parentQuery.data?.assigneeId ?? NaN
  const membersQuery = useTeamMembers(isRestrictedToParentTeam ? parentTeamId : NaN)
  const peopleQuery = usePeople(!isDepartmentAssigned && !isTeamAssigned && !isRestrictedToParentTeam)

  const personOptions = isRestrictedToParentTeam
    ? (membersQuery.data ?? []).map((member) => ({ id: member.personId, name: member.fullName }))
    : (peopleQuery.data ?? []).map((person) => ({ id: person.id, name: person.fullName }))
  const personOptionsLoading = isRestrictedToParentTeam ? membersQuery.isLoading : peopleQuery.isLoading

  const isSameOwner = isDepartmentAssigned
    ? newDepartmentId !== '' && Number(newDepartmentId) === task.assigneeId
    : isTeamAssigned
      ? newTeamId !== '' && Number(newTeamId) === task.assigneeId
      : newPersonId !== '' && Number(newPersonId) === task.assigneeId

  const hasTarget = isDepartmentAssigned ? newDepartmentId !== '' : isTeamAssigned ? newTeamId !== '' : newPersonId !== ''
  const isValid = hasTarget && !isSameOwner && reason.trim() !== '' && currentUser !== null

  function handleClose() {
    setNewTeamId('')
    setNewPersonId('')
    setNewDepartmentId('')
    setReason('')
    onClose()
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!isValid || !currentUser) return

    reassign.mutate(
      {
        newTeamId: isTeamAssigned ? Number(newTeamId) : undefined,
        newPersonId: !isDepartmentAssigned && !isTeamAssigned ? Number(newPersonId) : undefined,
        newDepartmentId: isDepartmentAssigned ? Number(newDepartmentId) : undefined,
        reassignedById: currentUser.id,
        reason: reason.trim(),
      },
      { onSuccess: handleClose },
    )
  }

  return (
    <Modal open={open} onClose={handleClose} title="Reassign task">
      <form className={styles.form} onSubmit={handleSubmit}>
        {isDepartmentAssigned ? (
          <SelectField
            label="New department"
            value={newDepartmentId}
            onChange={setNewDepartmentId}
            placeholder={departmentsQuery.isLoading ? 'Loading…' : 'Select a department'}
            options={(departmentsQuery.data ?? [])
              .filter((d) => d.id !== task.assigneeId)
              .map((d) => ({ label: d.name, value: String(d.id) }))}
          />
        ) : isTeamAssigned ? (
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
