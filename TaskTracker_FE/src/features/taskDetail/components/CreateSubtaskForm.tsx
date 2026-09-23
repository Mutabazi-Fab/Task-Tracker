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
import { maxAssignableDate, minAssignableDate } from '../../../lib/dateLimits'
import { InlineSubtasksField, type InlineSubtaskRow } from '../../tasks/components/InlineSubtasksField'
import { SourceCategoryField } from '../../tasks/components/SourceCategoryField'
import { SourceDetailField } from '../../tasks/components/SourceDetailField'
import type { CreateSubtaskRequest, TaskSeverity, TaskSource } from '../../../types/task.types'
import styles from '../../tasks/components/CreateTaskForm.module.css'

type AssigneeKind = 'TEAM' | 'INDIVIDUAL'

const ASSIGNEE_KIND_OPTIONS: { label: string; value: AssigneeKind }[] = [
  { label: 'Team', value: 'TEAM' },
  { label: 'Individual', value: 'INDIVIDUAL' },
]

// Executive/Super Admin only — the backend rejects a non-Executive creator's attempt to set severity.
const SEVERITY_OPTIONS: { label: string; value: TaskSeverity }[] = [
  { label: 'Low', value: 'LOW' },
  { label: 'Medium', value: 'MEDIUM' },
  { label: 'High', value: 'HIGH' },
  { label: 'Critical', value: 'CRITICAL' },
]

interface CreateSubtaskFormProps {
  /** Ordinary leaf case: parent is TEAM-assigned — assignees scoped to that team's members, always individual. */
  teamId: number
  /** True only when the parent is a Department-assigned Executive task — this is its
   *  "implementation task", team- or individual-assigned like a brand-new top-level task,
   *  except the team picker stays scoped to departmentId. teamId is unused in this mode. */
  isDepartmentImplementation?: boolean
  /** Scopes the team picker to this Department's own teams only. */
  departmentId?: number
  /** The immediate parent's own source/sourceLabel, when it has one — inherited and locked
   *  here (see sourceInherited) so the chain of custody stays visible. undefined/null falls
   *  back to the normal editable Source picker. */
  parentSource?: TaskSource | null
  parentSourceLabel?: string | null
  /** subtasks is whatever InlineSubtasksField collected (department-implementation case
   *  only, possibly empty) — the caller creates this task first, then loops over these to
   *  create one leaf subtask per filled row underneath it. */
  onSubmit: (payload: CreateSubtaskRequest, subtasks: InlineSubtaskRow[], documents: File[]) => void
  onCancel: () => void
  submitting: boolean
}

/** createdById is always the logged-in person — the backend still checks authorization, but there's no reason to ask when we already know who's here. */
export function CreateSubtaskForm({
  teamId,
  isDepartmentImplementation = false,
  departmentId,
  parentSource,
  parentSourceLabel,
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
  // Locked once the immediate parent already carries a source — visible but not overwritable here.
  const sourceInherited = !!parentSource
  const [source, setSource] = useState<TaskSource | ''>(parentSource ?? '')
  const [sourceLabel, setSourceLabel] = useState(parentSourceLabel ?? '')
  const [severity, setSeverity] = useState<TaskSeverity | ''>('')
  const [openingNote, setOpeningNote] = useState('')
  const [subtaskRows, setSubtaskRows] = useState<InlineSubtaskRow[]>([])
  const [documents, setDocuments] = useState<File[]>([])

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

  function handleSourceChange(value: string) {
    setSource(value)
    // Zigama has exactly one regulator — only fills when empty, so it never clobbers a typed value.
    if (value.toLowerCase() === 'regulator' && sourceLabel.trim() === '') {
      setSourceLabel('BNR')
    }
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

      {sourceInherited ? (
        <TextField label="Source (set by the CEO)" value={source} onChange={() => {}} disabled />
      ) : (
        <SourceCategoryField value={source} onChange={handleSourceChange} />
      )}
      {source && sourceInherited && (
        <TextField
          label="Source detail (set by the CEO)"
          value={sourceLabel}
          onChange={setSourceLabel}
          placeholder="e.g. Director Maj. Musoni, GPO, E&Y, Board of Directors"
          disabled
        />
      )}
      {source && !sourceInherited && (
        <SourceDetailField value={sourceLabel} onChange={setSourceLabel} label="Source detail" />
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
          {submitting ? 'Creating…' : isDepartmentImplementation ? 'Create implementation task' : 'Create subtask'}
        </Button>
      </div>
    </form>
  )
}
