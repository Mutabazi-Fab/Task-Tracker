import { useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { ErrorMessage } from '../../../components/ui/ErrorMessage'
import { SelectField } from '../../../components/ui/SelectField'
import { TextField } from '../../../components/ui/TextField'
import { useAuth } from '../../auth/useAuth'
import { useChangeRole } from '../hooks/useChangeRole'
import { useSetPersonActive } from '../hooks/useSetPersonActive'
import type { Person, Role } from '../../../types/person.types'
import styles from './PersonAdminControls.module.css'

const ROLE_OPTIONS: { label: string; value: Role }[] = [
  { label: 'Member', value: 'MEMBER' },
  { label: 'Director', value: 'DIRECTOR' },
  { label: 'Super Admin', value: 'SUPER_ADMIN' },
]

/**
 * Super-Admin-only — not rendered at all otherwise (see PersonProfilePage). The backend
 * still enforces the real rules (can't remove the last Super Admin, can't deactivate
 * yourself) — this just surfaces whatever error that produces rather than duplicating
 * the logic client-side.
 */
export function PersonAdminControls({ person }: { person: Person }) {
  const { currentUser } = useAuth()
  const changeRole = useChangeRole(person.id)
  const setActive = useSetPersonActive(person.id)

  const [newRole, setNewRole] = useState<Role>(person.role ?? 'MEMBER')
  const [roleReason, setRoleReason] = useState('')
  const [activeReason, setActiveReason] = useState('')

  if (!currentUser) return null

  function handleRoleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!currentUser || newRole === person.role) return
    changeRole.mutate(
      { newRole, changedById: currentUser.id, reason: roleReason.trim() || undefined },
      { onSuccess: () => setRoleReason('') },
    )
  }

  function handleActiveToggle() {
    if (!currentUser) return
    setActive.mutate({ active: !person.active, changedById: currentUser.id, reason: activeReason.trim() || undefined })
  }

  return (
    <Card>
      <div className={styles.wrap}>
        <span>Admin controls</span>

        <form className={styles.row} onSubmit={handleRoleSubmit}>
          <div className={styles.field}>
            <SelectField
              label="Role"
              value={newRole}
              onChange={(v) => setNewRole(v as Role)}
              options={ROLE_OPTIONS.map((o) => ({ label: o.label, value: o.value }))}
            />
          </div>
          <TextField label="Reason (optional)" value={roleReason} onChange={setRoleReason} placeholder="Why this change" />
          <Button type="submit" variant="secondary" disabled={newRole === person.role || changeRole.isPending}>
            {changeRole.isPending ? 'Updating…' : 'Update role'}
          </Button>
        </form>
        {changeRole.isError && <ErrorMessage message={changeRole.error.message} />}

        <div className={styles.row}>
          <span className={`${styles.statusBadge} ${person.active ? styles.statusActive : styles.statusInactive}`}>
            {person.active ? 'Active' : 'Deactivated'}
          </span>
          <TextField
            label="Reason (optional)"
            value={activeReason}
            onChange={setActiveReason}
            placeholder="Why this change"
          />
          <Button type="button" variant={person.active ? 'ghost' : 'secondary'} onClick={handleActiveToggle} disabled={setActive.isPending}>
            {setActive.isPending ? 'Saving…' : person.active ? 'Deactivate account' : 'Reactivate account'}
          </Button>
        </div>
        {setActive.isError && <ErrorMessage message={setActive.error.message} />}
      </div>
    </Card>
  )
}
