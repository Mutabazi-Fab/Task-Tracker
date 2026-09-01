import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { Button } from '../../components/ui/Button'
import { ErrorMessage } from '../../components/ui/ErrorMessage'
import { TextField } from '../../components/ui/TextField'
import { ROUTES } from '../../app/routes'
import { AuthLayout } from './components/AuthLayout'
import { useAuth } from './useAuth'
import type { ApiError } from '../../api/axiosClient'
import styles from './components/AuthLayout.module.css'
import verifyStyles from './VerifyEmailPage.module.css'

/** Reached from ForgotPasswordPage, which sends the email along via router state — falls
 *  back to asking for it if someone lands here directly (a refresh, a bad link). */
export function ResetPasswordPage() {
  const { forgotPassword, resetPassword } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const stateEmail = (location.state as { email?: string } | null)?.email ?? ''

  const [email, setEmail] = useState(stateEmail)
  const [code, setCode] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [resendMessage, setResendMessage] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [resending, setResending] = useState(false)

  const isValid = email.trim() !== '' && code.trim() !== '' && newPassword.length >= 8

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!isValid || submitting) return

    setSubmitting(true)
    setError(null)
    setResendMessage(null)
    try {
      const auth = await resetPassword({ email: email.trim(), code: code.trim(), newPassword })
      if (auth.token) {
        navigate(ROUTES.dashboard, { replace: true })
      }
    } catch (err) {
      setError((err as ApiError).message)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleResend() {
    if (email.trim() === '' || resending) return

    setResending(true)
    setError(null)
    setResendMessage(null)
    try {
      await forgotPassword({ email: email.trim() })
      setResendMessage('If an account with that email exists, a new code has been sent.')
    } finally {
      setResending(false)
    }
  }

  return (
    <AuthLayout
      title="Reset Password"
      subtitle="Enter the code we sent you and choose a new password"
      footerText="Remembered it?"
      footerLinkTo={ROUTES.login}
      footerLinkLabel="Sign in"
    >
      <form className={styles.form} onSubmit={handleSubmit}>
        {stateEmail === '' && (
          <TextField
            label="Email address"
            type="email"
            value={email}
            onChange={setEmail}
            placeholder="you@example.com"
            required
          />
        )}
        {stateEmail !== '' && <p className={verifyStyles.emailNotice}>Code sent to {stateEmail}</p>}

        <TextField
          label="Reset code"
          value={code}
          onChange={setCode}
          placeholder="6-digit code"
          inputMode="numeric"
          maxLength={6}
          required
        />

        <TextField
          label="New password"
          type="password"
          value={newPassword}
          onChange={setNewPassword}
          placeholder="At least 8 characters"
          autoComplete="new-password"
          required
        />

        {error && <ErrorMessage message={error} />}
        {resendMessage && <p className={verifyStyles.resendNotice}>{resendMessage}</p>}

        <Button type="submit" disabled={!isValid || submitting}>
          {submitting ? 'Resetting…' : 'Reset password'}
        </Button>

        <button type="button" className={verifyStyles.resendButton} onClick={handleResend} disabled={resending}>
          {resending ? 'Sending…' : "Didn't get a code? Resend"}
        </button>
      </form>
    </AuthLayout>
  )
}
