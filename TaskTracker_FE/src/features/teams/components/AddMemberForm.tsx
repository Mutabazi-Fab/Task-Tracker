import { useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { SelectField } from '../../../components/ui/SelectField'
import { TextField } from '../../../components/ui/TextField'
import { useAuth } from '../../auth/useAuth'
import { usePeople } from '../../people/hooks/usePeople'
import type { AddTeamMemberRequest } from '../../../types/team.types'
import type { TeamMember } from '../../../types/team.types'
import styles from './CreateTeamForm.module.css'

interface AddMemberFormProps {
  existingMembers: TeamMember[]
  onSubmit: (payload: AddTeamMemberRequest) => void
  onCancel: () => void
  submitting: boolean
}

/** Every add requires a reason — no add can be submitted without one. */
export function AddMemberForm({ existingMembers, onSubmit, onCancel, submitting }: AddMemberFormProps) {
  const { currentUser } = useAuth()
  const peopleQuery = usePeople()

  const [personId, setPersonId] = useState('')
  const [reason, setReason] = useState('')

  const existingIds = new Set(existingMembers.map((m) => m.personId))
  const candidates = (peopleQuery.data ?? []).filter((person) => !existingIds.has(person.id))

  const isValid = personId !== '' && reason.trim() !== '' && currentUser !== null

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!isValid || !currentUser) return

    onSubmit({ personId: Number(personId), changedById: currentUser.id, reason: reason.trim() })
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      <SelectField
        label="Person"
        value={personId}
        onChange={setPersonId}
        placeholder={peopleQuery.isLoading ? 'Loading…' : 'Select a person'}
        options={candidates.map((person) => ({ label: person.fullName, value: String(person.id) }))}
      />

      <TextField
        label="Reason"
        value={reason}
        onChange={setReason}
        placeholder="Why this person is joining the team"
        required
      />

      <div className={styles.actions}>
        <Button type="button" variant="ghost" onClick={onCancel} disabled={submitting}>
          Cancel
        </Button>
        <Button type="submit" variant="primary" disabled={!isValid || submitting}>
          {submitting ? 'Adding…' : 'Add member'}
        </Button>
      </div>
    </form>
  )
}
