import { Button } from '../../../components/ui/Button'
import { SelectField } from '../../../components/ui/SelectField'
import { TextField } from '../../../components/ui/TextField'
import { useTeamMembers } from '../../teams/hooks/useTeamMembers'
import formStyles from './CreateTaskForm.module.css'
import styles from './InlineSubtasksField.module.css'

export interface InlineSubtaskRow {
  personId: string
  title: string
}

interface InlineSubtasksFieldProps {
  teamId: number
  rows: InlineSubtaskRow[]
  onChange: (rows: InlineSubtaskRow[]) => void
}

/** Shown right under the team picker once a team's actually been chosen (see CreateTaskForm
 *  and CreateSubtaskForm's implementation-task case) — lets whoever's assigning work to
 *  that team break it straight into individual subtasks in the same step, instead of a
 *  separate trip to "Add subtask" per member afterward.
 *
 *  Entirely optional: submitting with zero rows here still creates the team task on its
 *  own, exactly as before this existed — a half-filled row (a person with no title, or a
 *  title with nobody picked) is simply skipped rather than blocking submission. Each
 *  properly filled row becomes its own createSubtask call, made only after the team task
 *  itself is created (its id is what a subtask needs) — see CreateTaskModal/
 *  CreateSubtaskModal, which own that follow-up sequencing; this component only collects
 *  the rows. */
export function InlineSubtasksField({ teamId, rows, onChange }: InlineSubtasksFieldProps) {
  const membersQuery = useTeamMembers(teamId)
  const memberOptions = (membersQuery.data ?? []).map((m) => ({ label: m.fullName, value: String(m.personId) }))

  function updateRow(index: number, patch: Partial<InlineSubtaskRow>) {
    onChange(rows.map((row, i) => (i === index ? { ...row, ...patch } : row)))
  }

  function removeRow(index: number) {
    onChange(rows.filter((_, i) => i !== index))
  }

  function addRow() {
    onChange([...rows, { personId: '', title: '' }])
  }

  return (
    <div className={formStyles.field}>
      <span className={formStyles.label}>Subtasks (optional)</span>
      {rows.length > 0 && (
        <div className={styles.rows}>
          {rows.map((row, index) => (
            <div key={index} className={styles.row}>
              <SelectField
                value={row.personId}
                onChange={(v) => updateRow(index, { personId: v })}
                placeholder={membersQuery.isLoading ? 'Loading…' : 'Assignee'}
                options={memberOptions}
              />
              <TextField value={row.title} onChange={(v) => updateRow(index, { title: v })} placeholder="Subtask title" />
              <button
                type="button"
                className={styles.remove}
                onClick={() => removeRow(index)}
                aria-label="Remove this subtask"
              >
                ✕
              </button>
            </div>
          ))}
        </div>
      )}
      <Button type="button" variant="ghost" onClick={addRow}>
        + Add subtask
      </Button>
    </div>
  )
}
