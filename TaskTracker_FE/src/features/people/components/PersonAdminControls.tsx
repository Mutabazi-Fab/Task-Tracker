import { useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { ErrorMessage } from '../../../components/ui/ErrorMessage'
import { Icon } from '../../../components/ui/Icon'
import { SelectField } from '../../../components/ui/SelectField'
import { TextField } from '../../../components/ui/TextField'
import { formatDateTime } from '../../../lib/formatDate'
import { useAuth } from '../../auth/useAuth'
import { useChangeRole } from '../hooks/useChangeRole'
import { useDismissPasswordResetRequest } from '../hooks/useDismissPasswordResetRequest'
import { useResetTotp } from '../hooks/useResetTotp'
import { useSetPassword } from '../hooks/useSetPassword'
import { useSetPersonActive } from '../hooks/useSetPersonActive'
import type { Person, Role } from '../../../types/person.types'
import styles from './PersonAdminControls.module.css'

const MIN_PASSWORD_LENGTH = 8

const ROLE_OPTIONS: { label: string; value: Role }[] = [
  { label: 'Member', value: 'MEMBER' },
  { label: 'Director', value: 'DIRECTOR' },
  { label: 'Executive', value: 'EXECUTIVE' },
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
  const setPassword = useSetPassword(person.id)
  const dismissRequest = useDismissPasswordResetRequest(person.id)
  const resetTotp = useResetTotp(person.id)

  const [newRole, setNewRole] = useState<Role>(person.role ?? 'MEMBER')
  const [roleReason, setRoleReason] = useState('')
  const [activeReason, setActiveReason] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [showNewPassword, setShowNewPassword] = useState(false)
  const [passwordReason, setPasswordReason] = useState('')
  const [totpResetReason, setTotpResetReason] = useState('')

  if (!currentUser) return null

  const canUpdateRole = newRole !== person.role && roleReason.trim() !== ''
  const canToggleActive = activeReason.trim() !== ''
  const canSetPassword = newPassword.length >= MIN_PASSWORD_LENGTH && passwordReason.trim() !== ''
  const canResetTotp = totpResetReason.trim() !== ''

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

  function handleSetPassword() {
    if (!currentUser || !canSetPassword) return
    setPassword.mutate(
      { changedById: currentUser.id, newPassword, reason: passwordReason.trim() },
      { onSuccess: () => { setNewPassword(''); setPasswordReason('') } },
    )
  }

  function handleDismissRequest() {
    if (!currentUser) return
    dismissRequest.mutate({ changedById: currentUser.id })
  }

  function handleResetTotp() {
    if (!currentUser || !canResetTotp) return
    resetTotp.mutate(
      { changedById: currentUser.id, reason: totpResetReason.trim() },
      { onSuccess: () => setTotpResetReason('') },
    )
  }

  return (
    <Card>
      <div className={styles.wrap}>
        <span className={styles.heading}>Admin controls</span>

        <div className={styles.section}>
          <span className={styles.sectionLabel}>Change role</span>
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
        </div>

        <div className={styles.section}>
          <span className={styles.sectionLabel}>Account status</span>
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

        <div className={styles.section}>
          <span className={styles.sectionLabel}>Password</span>

          {person.pendingPasswordResetRequestedAt && (
            <div className={styles.pendingRequestNotice}>
              <span>
                Requested a new password on {formatDateTime(person.pendingPasswordResetRequestedAt)}.
              </span>
              <button
                type="button"
                className={styles.dismissButton}
                onClick={handleDismissRequest}
                disabled={dismissRequest.isPending}
              >
                {dismissRequest.isPending ? 'Dismissing…' : 'Dismiss'}
              </button>
            </div>
          )}
          {dismissRequest.isError && <ErrorMessage message={dismissRequest.error.message} />}

          <TextField
            label="New password"
            type={showNewPassword ? 'text' : 'password'}
            value={newPassword}
            onChange={setNewPassword}
            placeholder={`At least ${MIN_PASSWORD_LENGTH} characters`}
            autoComplete="new-password"
            required
            trailing={
              <button
                type="button"
                className={styles.eyeButton}
                onClick={() => setShowNewPassword((v) => !v)}
                aria-label={showNewPassword ? 'Hide password' : 'Show password'}
              >
                <Icon name={showNewPassword ? 'eyeOff' : 'eye'} size={16} />
              </button>
            }
          />
          <div className={styles.row}>
            <TextField
              label="Reason"
              value={passwordReason}
              onChange={setPasswordReason}
              placeholder="e.g. user forgot password, verified by phone"
              required
            />
            <Button
              type="button"
              variant="primary"
              onClick={handleSetPassword}
              disabled={!canSetPassword || setPassword.isPending}
            >
              {setPassword.isPending ? 'Saving…' : 'Set new password'}
            </Button>
          </div>
          {setPassword.isError && <ErrorMessage message={setPassword.error.message} />}
          {setPassword.isSuccess && (
            <p className={styles.resetSentNotice}>
              Password set. Share it with {person.fullName} directly — nothing is emailed.
            </p>
          )}
        </div>

        {person.totpEnabled && (
          <div className={styles.section}>
            <span className={styles.sectionLabel}>Two-factor authentication</span>
            <div className={styles.row}>
              <TextField
                label="Reason"
                value={totpResetReason}
                onChange={setTotpResetReason}
                placeholder="e.g. lost or replaced phone"
                required
              />
              <button
                type="button"
                className={styles.dangerButton}
                onClick={handleResetTotp}
                disabled={!canResetTotp || resetTotp.isPending}
              >
                {resetTotp.isPending ? 'Resetting…' : 'Reset TOTP'}
              </button>
            </div>
            {resetTotp.isError && <ErrorMessage message={resetTotp.error.message} />}
            {resetTotp.isSuccess && (
              <p className={styles.resetSentNotice}>
                TOTP reset — they'll set up a new authenticator with a fresh QR code next time they log in.
              </p>
            )}
          </div>
        )}
      </div>
    </Card>
  )
}
