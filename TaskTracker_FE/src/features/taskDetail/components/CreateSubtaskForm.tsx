import { useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { ErrorMessage } from '../../../components/ui/ErrorMessage'
import { SegmentedControl } from '../../../components/ui/SegmentedControl'
import { SelectField } from '../../../components/ui/SelectField'
import { TextField } from '../../../components/ui/TextField'
import { useAuth } from '../../auth/useAuth'
import { useTeamMembers } from '../../teams/hooks/useTeamMembers'
import { useTeams } from '../../teams/hooks/useTeams'
import { usePeople } from '../../people/hooks/usePeople'
import { maxAssignableDate } from '../../../lib/dateLimits'
import { InlineSubtasksField, type InlineSubtaskRow } from '../../tasks/components/InlineSubtasksField'
import type { CreateSubtaskRequest, TaskSeverity, TaskSource } from '../../../types/task.types'
import styles from '../../tasks/components/CreateTaskForm.module.css'

type AssigneeKind = 'TEAM' | 'INDIVIDUAL'

const ASSIGNEE_KIND_OPTIONS: { label: string; value: AssigneeKind }[] = [
  { label: 'Team', value: 'TEAM' },
  { label: 'Individual', value: 'INDIVIDUAL' },
]

const SOURCE_OPTIONS: { label: string; value: TaskSource }[] = [
  { label: 'Initiative', value: 'INITIATIVE' },
  { label: 'Auditor', value: 'AUDITOR' },
  { label: 'Regulator', value: 'REGULATOR' },
  { label: 'Board', value: 'BOARD' },
]

// Executive/Super Admin only — the backend rejects a non-Executive creator's attempt to
// set severity, so a plain Director/Team Leader never sees a picker that'd just be
// rejected.
const SEVERITY_OPTIONS: { label: string; value: TaskSeverity }[] = [
  { label: 'Low', value: 'LOW' },
  { label: 'Medium', value: 'MEDIUM' },
  { label: 'High', value: 'HIGH' },
  { label: 'Critical', value: 'CRITICAL' },
]

interface CreateSubtaskFormProps {
  /** The ordinary leaf case: parent is TEAM-assigned (a plain top-level task, or a depth-1
   *  Department implementation task) — assignees are scoped to that team's members, never
   *  anyone outside it, and always individual. */
  teamId: number
  /** True only when the parent is a Department-assigned Executive task — this is its
   *  "implementation task", team- or individual-assigned, exactly like creating a
   *  brand-new top-level task (see CreateTaskForm), except the team picker stays scoped to
   *  that same Department (see departmentId) rather than every team org-wide. teamId above
   *  is unused in this mode. */
  isDepartmentImplementation?: boolean
  /** The parent Department task's own Department id — scopes the team picker to that
   *  Department's teams only, so e.g. a task assigned to Cybersecurity only offers
   *  Cybersecurity's own teams, never Finance's or IT's. */
  departmentId?: number
  /** subtasks is whatever InlineSubtasksField collected when assigneeKind is TEAM
   *  (department-implementation case only — the ordinary leaf case has nobody left to
   *  break the work down further into) — possibly empty. The caller (CreateSubtaskModal)
   *  creates this implementation task first, then loops over these to create one leaf
   *  subtask per filled row underneath it. */
  onSubmit: (payload: CreateSubtaskRequest, subtasks: InlineSubtaskRow[]) => void
  onCancel: () => void
  submitting: boolean
}

/** createdById is always the logged-in person — the backend still checks authorization
 *  (this team's leader or a Director/Super Admin for the leaf case; that Department's
 *  head Director or an Executive/Super Admin for the implementation-task case), but
 *  there's no reason to ask when we already know who's here. */
export function CreateSubtaskForm({
  teamId,
  isDepartmentImplementation = false,
  departmentId,
  onSubmit,
  onCancel,
  submitting,
}: CreateSubtaskFormProps) {
  const { currentUser, isExecutive } = useAuth()
  const membersQuery = useTeamMembers(teamId, !isDepartmentImplementation)
  const teamsQuery = useTeams()
  const peopleQuery = usePeople()
  const departmentTeams = (teamsQuery.data ?? []).filter((team) => team.departmentId === departmentId)
  const departmentPeople = (peopleQuery.data ?? []).filter((person) => person.departmentId === departmentId)

  const [assigneeKind, setAssigneeKind] = useState<AssigneeKind>('TEAM')
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [assignedPersonId, setAssignedPersonId] = useState('')
  const [assignedTeamId, setAssignedTeamId] = useState('')
  const [dateAssigned, setDateAssigned] = useState('')
  const [deadline, setDeadline] = useState('')
  const [source, setSource] = useState<TaskSource | ''>('')
  const [sourceLabel, setSourceLabel] = useState('')
  const [severity, setSeverity] = useState<TaskSeverity | ''>('')
  const [openingNote, setOpeningNote] = useState('')
  const [subtaskRows, setSubtaskRows] = useState<InlineSubtaskRow[]>([])

  function handleAssigneeKindChange(next: AssigneeKind) {
    setAssigneeKind(next)
    setAssignedPersonId('')
    setAssignedTeamId('')
    setSubtaskRows([])
  }

  function handleTeamChange(next: string) {
    setAssignedTeamId(next)
    // A different team means a different roster — any rows picked against the old one
    // would point at people who aren't even on this team.
    setSubtaskRows([])
  }

  const hasTarget = isDepartmentImplementation
    ? assigneeKind === 'TEAM' ? assignedTeamId !== '' : assignedPersonId !== ''
    : assignedPersonId !== ''
  const isDeadlineBeforeAssignment = dateAssigned !== '' && deadline !== '' && deadline < dateAssigned
  const isValid =
    title.trim() !== '' &&
    hasTarget &&
    dateAssigned !== '' &&
    deadline !== '' &&
    !isDeadlineBeforeAssignment &&
    openingNote.trim() !== '' &&
    currentUser !== null

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!isValid || !currentUser) return

    const isTeamImplementation = isDepartmentImplementation && assigneeKind === 'TEAM'

    onSubmit(
      {
        title: title.trim(),
        description: description.trim() || undefined,
        createdById: currentUser.id,
        assignedPersonId:
          !isDepartmentImplementation || assigneeKind === 'INDIVIDUAL' ? Number(assignedPersonId) || undefined : undefined,
        assignedTeamId: isTeamImplementation ? Number(assignedTeamId) : undefined,
        dateAssigned,
        deadline,
        source: source || undefined,
        sourceLabel: source && sourceLabel.trim() ? sourceLabel.trim() : undefined,
        severity: isExecutive && severity ? severity : undefined,
        openingNote: openingNote.trim(),
      },
      // Only meaningful for a Team-assigned implementation task — a half-filled row
      // (missing either the person or the title) is dropped rather than blocking submission.
      isTeamImplementation ? subtaskRows.filter((r) => r.personId !== '' && r.title.trim() !== '') : [],
    )
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      <TextField label="Title" value={title} onChange={setTitle} placeholder="What needs doing" required />

      <TextField label="Description" value={description} onChange={setDescription} placeholder="Optional detail" />

      {isDepartmentImplementation ? (
        <>
          <div className={styles.field}>
            <span className={styles.label}>Assign to</span>
            <SegmentedControl options={ASSIGNEE_KIND_OPTIONS} value={assigneeKind} onChange={handleAssigneeKindChange} />
          </div>
          {assigneeKind === 'TEAM' ? (
            <>
              <SelectField
                label="Assigned team"
                value={assignedTeamId}
                onChange={handleTeamChange}
                placeholder={teamsQuery.isLoading ? 'Loading…' : 'Select a team'}
                options={departmentTeams.map((team) => ({ label: team.name, value: String(team.id) }))}
              />
              {assignedTeamId !== '' && (
                <InlineSubtasksField teamId={Number(assignedTeamId)} rows={subtaskRows} onChange={setSubtaskRows} />
              )}
            </>
          ) : (
            <SelectField
              label="Assigned person"
              value={assignedPersonId}
              onChange={setAssignedPersonId}
              placeholder={peopleQuery.isLoading ? 'Loading…' : 'Select a person'}
              options={departmentPeople.map((person) => ({ label: person.fullName, value: String(person.id) }))}
            />
          )}
        </>
      ) : (
        <SelectField
          label="Assigned to"
          value={assignedPersonId}
          onChange={setAssignedPersonId}
          placeholder={membersQuery.isLoading ? 'Loading…' : 'Select a team member'}
          options={(membersQuery.data ?? []).map((member) => ({ label: member.fullName, value: String(member.personId) }))}
        />
      )}

      <TextField
        label="Date assigned"
        type="date"
        value={dateAssigned}
        onChange={setDateAssigned}
        max={maxAssignableDate()}
        required
      />

      <TextField
        label="Deadline"
        type="date"
        value={deadline}
        onChange={setDeadline}
        min={dateAssigned || undefined}
        required
      />
      {isDeadlineBeforeAssignment && <ErrorMessage message="Deadline can't be before the date assigned." />}

      <SelectField
        label="Source (optional)"
        value={source}
        onChange={(v) => setSource(v as TaskSource)}
        placeholder="Where this came from"
        options={SOURCE_OPTIONS.map((o) => ({ label: o.label, value: o.value }))}
      />
      {source && (
        <TextField
          label="Source detail"
          value={sourceLabel}
          onChange={setSourceLabel}
          placeholder="e.g. Director Musoni, GPO, E&Y, Board of Directors"
        />
      )}

      {isExecutive && (
        <SelectField
          label="Severity (optional)"
          value={severity}
          onChange={(v) => setSeverity(v as TaskSeverity)}
          placeholder="Not classified"
          options={SEVERITY_OPTIONS.map((o) => ({ label: o.label, value: o.value }))}
        />
      )}

      <TextField
        label="Opening note"
        value={openingNote}
        onChange={setOpeningNote}
        placeholder="Why this subtask starts at 0%"
        required
      />

      <div className={styles.actions}>
        <Button type="button" variant="ghost" onClick={onCancel} disabled={submitting}>
          Cancel
        </Button>
        <Button type="submit" variant="primary" disabled={!isValid || submitting}>
          {submitting ? 'Creating…' : 'Create subtask'}
        </Button>
      </div>
    </form>
  )
}
