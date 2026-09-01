import { useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { SelectField } from '../../../components/ui/SelectField'
import { TextField } from '../../../components/ui/TextField'
import { useAuth } from '../../auth/useAuth'
import type { CreatePersonRequest, Role } from '../../../types/person.types'
import styles from '../../teams/components/CreateTeamForm.module.css'

const ROLE_OPTIONS: { label: string; value: Role }[] = [
  { label: 'Member', value: 'MEMBER' },
  { label: 'Director', value: 'DIRECTOR' },
  { label: 'Super Admin', value: 'SUPER_ADMIN' },
]

interface CreatePersonFormProps {
  onSubmit: (payload: CreatePersonRequest) => void
  onCancel: () => void
  submitting: boolean
}

/** The role picker only appears for a Super Admin — a Director creating someone is
 *  always capped at Member server-side, so there's no point offering a choice that
 *  would just be rejected. */
export function CreatePersonForm({ onSubmit, onCancel, submitting }: CreatePersonFormProps) {
  const { currentUser, isSuperAdmin } = useAuth()

  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [jobTitle, setJobTitle] = useState('')
  const [rank, setRank] = useState('')
  const [role, setRole] = useState<Role>('MEMBER')

  const isValid = fullName.trim() !== '' && email.trim() !== '' && jobTitle.trim() !== '' && currentUser !== null

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!isValid || !currentUser) return

    onSubmit({
      fullName: fullName.trim(),
      email: email.trim(),
      jobTitle: jobTitle.trim(),
      rank: rank.trim() || undefined,
      createdById: currentUser.id,
      role: isSuperAdmin ? role : undefined,
    })
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      <TextField label="Full name" value={fullName} onChange={setFullName} placeholder="Their full name" required />
      <TextField
        label="Email"
        type="email"
        value={email}
        onChange={setEmail}
        placeholder="you@example.com"
        required
      />
      <TextField label="Job title" value={jobTitle} onChange={setJobTitle} placeholder="e.g. Backend Engineer" required />
      <TextField label="Rank (optional)" value={rank} onChange={setRank} placeholder="e.g. Captain" />

      {isSuperAdmin && (
        <SelectField
          label="Role"
          value={role}
          onChange={(v) => setRole(v as Role)}
          options={ROLE_OPTIONS.map((o) => ({ label: o.label, value: o.value }))}
        />
      )}

      <div className={styles.actions}>
        <Button type="button" variant="ghost" onClick={onCancel} disabled={submitting}>
          Cancel
        </Button>
        <Button type="submit" variant="primary" disabled={!isValid || submitting}>
          {submitting ? 'Creating…' : 'Create person'}
        </Button>
      </div>
    </form>
  )
}
