import { useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { Icon } from '../../../components/ui/Icon'
import { SelectField } from '../../../components/ui/SelectField'
import { TextField } from '../../../components/ui/TextField'
import { useAuth } from '../../auth/useAuth'
import { useDepartments } from '../../departments/hooks/useDepartments'
import type { CreatePersonRequest, Role } from '../../../types/person.types'
import styles from '../../teams/components/CreateTeamForm.module.css'

const ROLE_OPTIONS: { label: string; value: Role }[] = [
  { label: 'Member', value: 'MEMBER' },
  { label: 'Director', value: 'DIRECTOR' },
  { label: 'Executive', value: 'EXECUTIVE' },
  { label: 'Super Admin', value: 'SUPER_ADMIN' },
]

const MIN_PASSWORD_LENGTH = 8

interface CreatePersonFormProps {
  onSubmit: (payload: CreatePersonRequest) => void
  onCancel: () => void
  submitting: boolean
}

/** Only a Super Admin ever reaches this form (see PeopleListPage's gate on the "New person" button) —
 *  there's no public self-registration, so this is the only way a new account gets created, and it's
 *  created fully login-ready: whatever password is set here is what the person logs in with, so the
 *  Super Admin is expected to hand it to them directly afterward. */
export function CreatePersonForm({ onSubmit, onCancel, submitting }: CreatePersonFormProps) {
  const { currentUser } = useAuth()
  const departmentsQuery = useDepartments()

  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [jobTitle, setJobTitle] = useState('')
  const [rank, setRank] = useState('')
  const [role, setRole] = useState<Role>('MEMBER')
  const [departmentId, setDepartmentId] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)

  const isValid =
    fullName.trim() !== '' &&
    email.trim() !== '' &&
    jobTitle.trim() !== '' &&
    departmentId !== '' &&
    password.length >= MIN_PASSWORD_LENGTH &&
    currentUser !== null

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!isValid || !currentUser) return

    onSubmit({
      fullName: fullName.trim(),
      email: email.trim(),
      jobTitle: jobTitle.trim(),
      rank: rank.trim() || undefined,
      createdById: currentUser.id,
      role,
      departmentId: Number(departmentId),
      password,
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

      <SelectField
        label="Department"
        value={departmentId}
        onChange={setDepartmentId}
        placeholder="Select a department"
        options={(departmentsQuery.data ?? []).map((d) => ({ label: d.name, value: String(d.id) }))}
      />

      <SelectField
        label="Role"
        value={role}
        onChange={(v) => setRole(v as Role)}
        options={ROLE_OPTIONS.map((o) => ({ label: o.label, value: o.value }))}
      />

      <TextField
        label="Password"
        type={showPassword ? 'text' : 'password'}
        value={password}
        onChange={setPassword}
        placeholder={`At least ${MIN_PASSWORD_LENGTH} characters`}
        autoComplete="new-password"
        required
        trailing={
          <button
            type="button"
            className={styles.eyeButton}
            onClick={() => setShowPassword((v) => !v)}
            aria-label={showPassword ? 'Hide password' : 'Show password'}
          >
            <Icon name={showPassword ? 'eyeOff' : 'eye'} size={16} />
          </button>
        }
      />

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
