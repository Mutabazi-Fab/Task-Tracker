import { useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { SelectField } from '../../../components/ui/SelectField'
import { TextField } from '../../../components/ui/TextField'
import { useAuth } from '../../auth/useAuth'
import { usePeople } from '../../people/hooks/usePeople'
import type { CreateDepartmentRequest } from '../../../types/department.types'
import styles from '../../teams/components/CreateTeamForm.module.css'

interface CreateDepartmentFormProps {
  onSubmit: (payload: CreateDepartmentRequest) => void
  onCancel: () => void
  submitting: boolean
}

/** Executive-or-above (see CreateDepartmentModal's caller — the CEO or Super Admin). A
 *  department's head must already hold the Director role or above — the picker only offers
 *  people who qualify, same "don't offer a choice that'd just be rejected" approach as
 *  CreatePersonForm's role picker. */
export function CreateDepartmentForm({ onSubmit, onCancel, submitting }: CreateDepartmentFormProps) {
  const { currentUser } = useAuth()
  const peopleQuery = usePeople()

  const [name, setName] = useState('')
  const [headDirectorId, setHeadDirectorId] = useState('')

  const directors = (peopleQuery.data ?? []).filter(
    (person) => person.role === 'DIRECTOR' || person.role === 'EXECUTIVE' || person.role === 'SUPER_ADMIN',
  )

  const isValid = name.trim() !== '' && headDirectorId !== '' && currentUser !== null

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!isValid || !currentUser) return

    onSubmit({
      name: name.trim(),
      headDirectorId: Number(headDirectorId),
      createdById: currentUser.id,
    })
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      <TextField label="Department name" value={name} onChange={setName} placeholder="e.g. Information Technology" required />

      <SelectField
        label="Head Director"
        value={headDirectorId}
        onChange={setHeadDirectorId}
        placeholder={directors.length === 0 ? 'No Director-level people yet' : 'Select a head'}
        options={directors.map((person) => ({ label: person.fullName, value: String(person.id) }))}
      />

      <div className={styles.actions}>
        <Button type="button" variant="ghost" onClick={onCancel} disabled={submitting}>
          Cancel
        </Button>
        <Button type="submit" variant="primary" disabled={!isValid || submitting}>
          {submitting ? 'Creating…' : 'Create department'}
        </Button>
      </div>
    </form>
  )
}
