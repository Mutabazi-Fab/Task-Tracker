import { useState } from 'react'
import { Button } from '../../components/ui/Button'
import { ErrorMessage } from '../../components/ui/ErrorMessage'
import { Modal } from '../../components/ui/Modal'
import { SuccessMessage } from '../../components/ui/SuccessMessage'
import { TextField } from '../../components/ui/TextField'
import { ROUTES } from '../../app/routes'
import { AuthLayout } from './components/AuthLayout'
import { useAuth } from './useAuth'
import type { ApiError } from '../../api/axiosClient'
import styles from './components/AuthLayout.module.css'
import forgotStyles from './ForgotPasswordPage.module.css'

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

/** Entirely offline: no email is ever sent. */
export function ForgotPasswordPage() {
  const { checkEmailForPasswordReset, createPasswordResetRequest } = useAuth()

  const [email, setEmail] = useState('')
  const [checking, setChecking] = useState(false)
  const [confirming, setConfirming] = useState(false)
  const [showConfirm, setShowConfirm] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [outcome, setOutcome] = useState<'created' | 'already-pending' | null>(null)

  const isValid = email.trim() !== ''

  async function handleCheck(e: React.FormEvent) {
    e.preventDefault()
    if (!isValid || checking) return

    const trimmed = email.trim()
    if (!EMAIL_PATTERN.test(trimmed)) {
      setError('Please enter a valid email address.')
      return
    }

    setChecking(true)
    setError(null)
    try {
      const result = await checkEmailForPasswordReset({ email: trimmed })
      if (result.exists) {
        setShowConfirm(true)
      } else {
        setError('No account was found with this email. Please check the email and try again.')
      }
    } catch (err) {
      setError((err as ApiError).message)
    } finally {
      setChecking(false)
    }
  }

  async function handleConfirmRequest() {
    if (confirming) return

    setConfirming(true)
    setError(null)
    try {
      const result = await createPasswordResetRequest({ email: email.trim() })
      setOutcome(result.status === 'ALREADY_PENDING' ? 'already-pending' : 'created')
      setShowConfirm(false)
    } catch (err) {
      setError((err as ApiError).message)
      setShowConfirm(false)
    } finally {
      setConfirming(false)
    }
  }

  if (outcome) {
    return (
      <AuthLayout title="Forgot Password" subtitle="" footerText="Remembered it?" footerLinkTo={ROUTES.login} footerLinkLabel="Sign in">
        <SuccessMessage
          message={
            outcome === 'already-pending'
              ? 'You already have a pending request awaiting the Super Admin.'
              : 'Request for a new password sent to the Super Admin.'
          }
        />
      </AuthLayout>
    )
  }

  return (
    <AuthLayout
      title="Forgot Password"
      subtitle="Enter your email to request a new password from the Super Admin"
      footerText="Remembered it?"
      footerLinkTo={ROUTES.login}
      footerLinkLabel="Sign in"
    >
      <form className={styles.form} onSubmit={handleCheck}>
        <TextField
          label="Email address"
          type="email"
          value={email}
          onChange={setEmail}
          placeholder="you@example.com"
          required
        />

        {error && <ErrorMessage message={error} />}

        <Button type="submit" disabled={!isValid || checking}>
          {checking ? 'Checking…' : 'Continue'}
        </Button>
      </form>

      <Modal open={showConfirm} onClose={() => setShowConfirm(false)} title="Request a new password?">
        <p className={forgotStyles.confirmText}>
          A Super Admin will be notified and can set a new password for you, which they'll
          give you directly.
        </p>
        <div className={forgotStyles.confirmActions}>
          <Button type="button" variant="ghost" onClick={() => setShowConfirm(false)} disabled={confirming}>
            Cancel
          </Button>
          <Button type="button" variant="primary" onClick={handleConfirmRequest} disabled={confirming}>
            {confirming ? 'Sending…' : 'Yes, request one'}
          </Button>
        </div>
      </Modal>
    </AuthLayout>
  )
}
