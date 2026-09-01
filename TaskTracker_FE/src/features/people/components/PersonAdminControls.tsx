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
 * the logic client-side. Reason is mandatory for both actions — enforced here (button
 * stays disabled without one) and server-side (the request is rejected regardless).
 */
export function PersonAdminControls({ person }: { person: Person }) {
  const { currentUser } = useAuth()
  const changeRole = useChangeRole(person.id)
  const setActive = useSetPersonActive(person.id)

  const [newRole, setNewRole] = useState<Role>(person.role ?? 'MEMBER')
  const [roleReason, setRoleReason] = useState('')
  const [activeReason, setActiveReason] = useState('')

  if (!currentUser) return null

  const canUpdateRole = newRole !== person.role && roleReason.trim() !== ''
  const canToggleActive = activeReason.trim() !== ''

  function handleRoleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!currentUser || !canUpdateRole) return
    changeRole.mutate(
      { newRole, changedById: currentUser.id, reason: roleReason.trim() },
      { onSuccess: () => setRoleReason('') },
    )
  }

  function handleActiveToggle() {
    if (!currentUser || !canToggleActive) return
    setActive.mutate({ active: !person.active, changedById: currentUser.id, reason: activeReason.trim() })
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
          <TextField label="Reason" value={roleReason} onChange={setRoleReason} placeholder="Why this change" required />
          <Button type="submit" variant="secondary" disabled={!canUpdateRole || changeRole.isPending}>
            {changeRole.isPending ? 'Updating…' : 'Update role'}
          </Button>
        </form>
        {changeRole.isError && <ErrorMessage message={changeRole.error.message} />}

        <div className={styles.row}>
          <TextField label="Reason" value={activeReason} onChange={setActiveReason} placeholder="Why this change" required />
          {person.active ? (
            <button
              type="button"
              className={styles.dangerButton}
              onClick={handleActiveToggle}
              disabled={!canToggleActive || setActive.isPending}
            >
              {setActive.isPending ? 'Saving…' : 'Deactivate account'}
            </button>
          ) : (
            <Button
              type="button"
              variant="secondary"
              onClick={handleActiveToggle}
              disabled={!canToggleActive || setActive.isPending}
            >
              {setActive.isPending ? 'Saving…' : 'Reactivate account'}
            </Button>
          )}
        </div>
        {setActive.isError && <ErrorMessage message={setActive.error.message} />}
      </div>
    </Card>
  )
}
