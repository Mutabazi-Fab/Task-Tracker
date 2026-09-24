import { useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { ErrorMessage } from '../../../components/ui/ErrorMessage'
import { SegmentedControl } from '../../../components/ui/SegmentedControl'
import { SelectField } from '../../../components/ui/SelectField'
import { TextField } from '../../../components/ui/TextField'
import { useAuth } from '../../auth/useAuth'
import { usePeople } from '../../people/hooks/usePeople'
import { useTeams } from '../../teams/hooks/useTeams'
import { useDepartments } from '../../departments/hooks/useDepartments'
import { maxAssignableDate, minAssignableDate } from '../../../lib/dateLimits'
import { InlineSubtasksField, type InlineSubtaskRow } from './InlineSubtasksField'
import { SourceCategoryField } from './SourceCategoryField'
import { SourceDetailField } from './SourceDetailField'
import type { CreateTaskRequest, TaskSeverity, TaskSource } from '../../../types/task.types'
import styles from './CreateTaskForm.module.css'

type AssigneeKind = 'TEAM' | 'INDIVIDUAL' | 'DEPARTMENT'

const ASSIGNEE_KIND_OPTIONS: { label: string; value: AssigneeKind }[] = [
  { label: 'Team', value: 'TEAM' },
  { label: 'Individual', value: 'INDIVIDUAL' },
]

// Executive/Super Admin only — appended to the options above when the caller qualifies, so a plain Director never sees a choice that'd be rejected server-side.
const DEPARTMENT_OPTION: { label: string; value: AssigneeKind } = { label: 'Department', value: 'DEPARTMENT' }

// Executive/Super Admin only — the backend rejects a non-Executive creator's attempt to set severity.
const SEVERITY_OPTIONS: { label: string; value: TaskSeverity }[] = [
  { label: 'Low', value: 'LOW' },
  { label: 'Medium', value: 'MEDIUM' },
  { label: 'High', value: 'HIGH' },
  { label: 'Critical', value: 'CRITICAL' },
]

interface CreateTaskFormProps {
  /** subtasks is whatever InlineSubtasksField collected (possibly empty) — the caller creates the team task first, then loops over these to create one leaf subtask per filled row. documents is uploaded the same way once the task exists. */
  onSubmit: (payload: CreateTaskRequest, subtasks: InlineSubtaskRow[], documents: File[]) => void
  onCancel: () => void
  submitting: boolean
}

/** Top-level (depth 0) tasks only — team, individual (behaves like a subtask itself, no rollup, no
 *  children), or Department (Executive-only; its head Director later turns it into a real
 *  "implementation task"). createdById is always the logged-in Director-or- above. */
export function CreateTaskForm({ onSubmit, onCancel, submitting }: CreateTaskFormProps) {
  const { currentUser, isExecutive } = useAuth()
  const teamsQuery = useTeams()
  const peopleQuery = usePeople()
  const departmentsQuery = useDepartments()

  const isCeo = currentUser?.role === 'EXECUTIVE'
  const assigneeKindOptions = isCeo
    ? [DEPARTMENT_OPTION]
    : isExecutive
      ? [...ASSIGNEE_KIND_OPTIONS, DEPARTMENT_OPTION]
      : ASSIGNEE_KIND_OPTIONS

  // A plain Director may only assign within the department they head (enforced server-side
  // too) — scoped here so they never see a choice that'd be rejected. Executive/Super Admin keep the full list.
  const assignableTeams = isExecutive
    ? (teamsQuery.data ?? [])
    : (teamsQuery.data ?? []).filter((team) => team.departmentId === currentUser?.departmentId)
  const assignablePeople = isExecutive
    ? (peopleQuery.data ?? [])
    : (peopleQuery.data ?? []).filter((person) => person.departmentId === currentUser?.departmentId)

  const [assigneeKind, setAssigneeKind] = useState<AssigneeKind>(() => (isCeo ? 'DEPARTMENT' : 'TEAM'))
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [assignedTeamId, setAssignedTeamId] = useState('')
  const [assignedPersonId, setAssignedPersonId] = useState('')
  const [assignedDepartmentId, setAssignedDepartmentId] = useState('')
  const [dateAssigned, setDateAssigned] = useState('')
  const [deadline, setDeadline] = useState('')
  const [source, setSource] = useState<TaskSource | ''>('')
  const [sourceLabel, setSourceLabel] = useState('')
  const [severity, setSeverity] = useState<TaskSeverity | ''>('')
  const [openingNote, setOpeningNote] = useState('')
  const [subtaskRows, setSubtaskRows] = useState<InlineSubtaskRow[]>([])
  const [documents, setDocuments] = useState<File[]>([])

  const hasTarget =
    assigneeKind === 'TEAM' ? assignedTeamId !== ''
    : assigneeKind === 'INDIVIDUAL' ? assignedPersonId !== ''
    : assignedDepartmentId !== ''
  const isDeadlineBeforeAssignment = dateAssigned !== '' && deadline !== '' && deadline < dateAssigned
  const isValid =
    title.trim() !== '' &&
    hasTarget &&
    dateAssigned !== '' &&
    deadline !== '' &&
    !isDeadlineBeforeAssignment &&
    openingNote.trim() !== '' &&
    currentUser !== null

  function handleAssigneeKindChange(next: AssigneeKind) {
    setAssigneeKind(next)
    setAssignedTeamId('')
    setAssignedPersonId('')
    setAssignedDepartmentId('')
    setSubtaskRows([])
  }

  function handleTeamChange(next: string) {
    setAssignedTeamId(next)
    // A different team means a different roster — any rows picked against the old one
    // would point at people who aren't even on this team.
    setSubtaskRows([])
  }

  function handleSourceChange(value: string) {
    setSource(value)
    // Zigama has exactly one regulator — don't make anyone type it. Only fills when the
    // field is currently empty, so it never clobbers something the user already typed.
    if (value.toLowerCase() === 'regulator' && sourceLabel.trim() === '') {
      setSourceLabel('BNR')
    }
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!isValid || !currentUser) return

    onSubmit(
      {
        title: title.trim(),
        description: description.trim() || undefined,
        createdById: currentUser.id,
        assignedTeamId: assigneeKind === 'TEAM' ? Number(assignedTeamId) : undefined,
        assignedPersonId: assigneeKind === 'INDIVIDUAL' ? Number(assignedPersonId) : undefined,
        assignedDepartmentId: assigneeKind === 'DEPARTMENT' ? Number(assignedDepartmentId) : undefined,
        dateAssigned,
        deadline,
        source: source || undefined,
        sourceLabel: source && sourceLabel.trim() ? sourceLabel.trim() : undefined,
        severity: isExecutive && severity ? severity : undefined,
        openingNote: openingNote.trim(),
      },
      // Only meaningful when assigning to a Team — a half-filled row (missing either the
      // person or the title) is dropped rather than blocking submission.
      assigneeKind === 'TEAM' ? subtaskRows.filter((r) => r.personId !== '' && r.title.trim() !== '') : [],
      documents,
    )
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      <TextField label="Title" value={title} onChange={setTitle} placeholder="What needs doing" required />

      <TextField label="Description" value={description} onChange={setDescription} placeholder="Optional detail" />

      <div className={styles.field}>
        <span className={styles.label}>Supporting documents (optional)</span>
        <input
          type="file"
          multiple
          onChange={(e) => setDocuments(e.target.files ? Array.from(e.target.files) : [])}
        />
      </div>

      {!isCeo && (
        <div className={styles.field}>
          <span className={styles.label}>Assign to</span>
          <SegmentedControl options={assigneeKindOptions} value={assigneeKind} onChange={handleAssigneeKindChange} />
        </div>
      )}

      {assigneeKind === 'TEAM' && (
        <>
          <SelectField
            label="Assigned team"
            value={assignedTeamId}
            onChange={handleTeamChange}
            placeholder={teamsQuery.isLoading ? 'Loading…' : 'Select a team'}
            options={assignableTeams.map((team) => ({ label: team.name, value: String(team.id) }))}
          />
          {assignedTeamId !== '' && (
            <InlineSubtasksField teamId={Number(assignedTeamId)} rows={subtaskRows} onChange={setSubtaskRows} />
          )}
        </>
      )}
      {assigneeKind === 'INDIVIDUAL' && (
        <SelectField
          label="Assigned person"
          value={assignedPersonId}
          onChange={setAssignedPersonId}
          placeholder={peopleQuery.isLoading ? 'Loading…' : 'Select a person'}
          options={assignablePeople.map((person) => ({ label: person.fullName, value: String(person.id) }))}
        />
      )}
      {assigneeKind === 'DEPARTMENT' && (
        <SelectField
          label="Assigned department"
          value={assignedDepartmentId}
          onChange={setAssignedDepartmentId}
          placeholder={departmentsQuery.isLoading ? 'Loading…' : 'Select a department'}
          options={(departmentsQuery.data ?? []).map((d) => ({ label: d.name, value: String(d.id) }))}
        />
      )}

      <TextField
        label="Date assigned"
        type="date"
        value={dateAssigned}
        onChange={setDateAssigned}
        min={minAssignableDate()}
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

      <SourceCategoryField value={source} onChange={handleSourceChange} />
      {source && <SourceDetailField value={sourceLabel} onChange={setSourceLabel} label="Source detail" />}

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
        placeholder="Why this task starts at 0%"
        required
      />

      <div className={styles.actions}>
        <Button type="button" variant="ghost" onClick={onCancel} disabled={submitting}>
          Cancel
        </Button>
        <Button type="submit" variant="primary" disabled={!isValid || submitting}>
          {submitting ? 'Creating…' : 'Create task'}
        </Button>
      </div>
    </form>
  )
}
