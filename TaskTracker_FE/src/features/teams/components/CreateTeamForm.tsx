import { useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { SelectField } from '../../../components/ui/SelectField'
import { TextField } from '../../../components/ui/TextField'
import { useAuth } from '../../auth/useAuth'
import { usePeople } from '../../people/hooks/usePeople'
import { useDepartments } from '../../departments/hooks/useDepartments'
import type { CreateTeamRequest } from '../../../types/team.types'
import styles from './CreateTeamForm.module.css'

interface CreateTeamFormProps {
  onSubmit: (payload: CreateTeamRequest) => void
  onCancel: () => void
  submitting: boolean
  /** Set when this form is opened from a specific Department's own page (see
   *  DepartmentPage) — locks the department to that one instead of offering the full
   *  org-wide picker (Executive/Super Admin) or falling back to the caller's own department
   *  (a plain Director), since it's already obvious from context which department this is. */
  fixedDepartmentId?: number
  fixedDepartmentName?: string
}

/** Director creates the team, picks its roster, and names one member as Team Leader —
 *  all in one request (leaderId must be one of memberIds).
 *
 *  A plain Director only ever heads one department, so they never get a department picker
 *  at all — it's silently their own, same as GET /tasks' department scoping. Only
 *  Executive/Super Admin (who aren't tied to a single department) get to choose, since
 *  they're the only ones actually able to stand up a team for someone else's department —
 *  enforced server-side in TeamServiceImpl.createTeam, not just hidden here. When opened
 *  from a Department's own page (fixedDepartmentId set), nobody gets a picker — it's locked
 *  to that department for every role, Executive/Super Admin included. */
export function CreateTeamForm({
  onSubmit,
  onCancel,
  submitting,
  fixedDepartmentId,
  fixedDepartmentName,
}: CreateTeamFormProps) {
  const { currentUser, isExecutive } = useAuth()
  const peopleQuery = usePeople()
  const departmentsQuery = useDepartments()

  const [name, setName] = useState('')
  const [memberIds, setMemberIds] = useState<number[]>([])
  const [leaderId, setLeaderId] = useState('')
  const [departmentId, setDepartmentId] = useState(() =>
    fixedDepartmentId != null
      ? String(fixedDepartmentId)
      : !isExecutive && currentUser?.departmentId
        ? String(currentUser.departmentId)
        : '',
  )

  function toggleMember(id: number) {
    setMemberIds((prev) => {
      const next = prev.includes(id) ? prev.filter((m) => m !== id) : [...prev, id]
      if (leaderId !== '' && !next.includes(Number(leaderId))) {
        setLeaderId('')
      }
      return next
    })
  }

  const isValid =
    name.trim() !== '' && memberIds.length > 0 && leaderId !== '' && departmentId !== '' && currentUser !== null

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!isValid || !currentUser) return

    onSubmit({
      name: name.trim(),
      createdById: currentUser.id,
      leaderId: Number(leaderId),
      memberIds,
      departmentId: Number(departmentId),
    })
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      <TextField label="Team name" value={name} onChange={setName} placeholder="e.g. Auditing App" required />

      {fixedDepartmentId != null ? (
        <div className={styles.field}>
          <span className={styles.label}>Department</span>
          <span className={styles.readOnlyValue}>{fixedDepartmentName ?? 'This department'}</span>
        </div>
      ) : isExecutive ? (
        <SelectField
          label="Department"
          value={departmentId}
          onChange={setDepartmentId}
          placeholder="Select a department"
          options={(departmentsQuery.data ?? []).map((d) => ({ label: d.name, value: String(d.id) }))}
        />
      ) : (
        <div className={styles.field}>
          <span className={styles.label}>Department</span>
          <span className={styles.readOnlyValue}>{currentUser?.departmentName ?? 'No department set'}</span>
        </div>
      )}

      <div className={styles.field}>
        <span className={styles.label}>Members</span>
        <div className={styles.memberList}>
          {(peopleQuery.data ?? []).map((person) => (
            <label key={person.id} className={styles.memberRow}>
              <input
                type="checkbox"
                checked={memberIds.includes(person.id)}
                onChange={() => toggleMember(person.id)}
              />
              {person.fullName}
            </label>
          ))}
        </div>
      </div>

      <SelectField
        label="Team leader"
        value={leaderId}
        onChange={setLeaderId}
        placeholder={memberIds.length === 0 ? 'Pick members first' : 'Select a leader'}
        options={(peopleQuery.data ?? [])
          .filter((person) => memberIds.includes(person.id))
          .map((person) => ({ label: person.fullName, value: String(person.id) }))}
      />

      <div className={styles.actions}>
        <Button type="button" variant="ghost" onClick={onCancel} disabled={submitting}>
          Cancel
        </Button>
        <Button type="submit" variant="primary" disabled={!isValid || submitting}>
          {submitting ? 'Creating…' : 'Create team'}
        </Button>
      </div>
    </form>
  )
}
