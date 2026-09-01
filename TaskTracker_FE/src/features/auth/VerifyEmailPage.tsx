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

/**
 * Reached either from signup (a brand-new account, emailVerified=false) or from login
 * (an account that signed up but never verified) — both send the email along via router
 * state. Falls back to asking for it if someone lands here directly (a refresh, a bad
 * link) rather than being stuck with nothing to submit.
 */
export function VerifyEmailPage() {
  const { verifyEmail, resendOtp } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const stateEmail = (location.state as { email?: string } | null)?.email ?? ''

  const [email, setEmail] = useState(stateEmail)
  const [otp, setOtp] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [resendMessage, setResendMessage] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [resending, setResending] = useState(false)

  const isValid = email.trim() !== '' && otp.trim() !== ''

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!isValid || submitting) return

    setSubmitting(true)
    setError(null)
    setResendMessage(null)
    try {
      const auth = await verifyEmail({ email: email.trim(), otp: otp.trim() })
      if (auth.emailVerified) {
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
      await resendOtp({ email: email.trim() })
      setResendMessage('A new code has been sent.')
    } catch (err) {
      setError((err as ApiError).message)
    } finally {
      setResending(false)
    }
  }

  return (
    <AuthLayout
      title="Verify Your Email"
      subtitle="Enter the code we sent to confirm it's really you"
      footerText="Already verified?"
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
          label="Verification code"
          value={otp}
          onChange={setOtp}
          placeholder="6-digit code"
          inputMode="numeric"
          maxLength={6}
          required
        />

        {error && <ErrorMessage message={error} />}
        {resendMessage && <p className={verifyStyles.resendNotice}>{resendMessage}</p>}

        <Button type="submit" disabled={!isValid || submitting}>
          {submitting ? 'Verifying…' : 'Verify'}
        </Button>

        <button type="button" className={verifyStyles.resendButton} onClick={handleResend} disabled={resending}>
          {resending ? 'Sending…' : "Didn't get a code? Resend"}
        </button>
      </form>
    </AuthLayout>
  )
}
