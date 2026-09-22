import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { Button } from '../../components/ui/Button'
import { ErrorMessage } from '../../components/ui/ErrorMessage'
import { Icon } from '../../components/ui/Icon'
import { TextField } from '../../components/ui/TextField'
import { ROUTES } from '../../app/routes'
import { AuthLayout } from './components/AuthLayout'
import { useAuth } from './useAuth'
import type { ApiError } from '../../api/axiosClient'
import styles from './components/AuthLayout.module.css'
import verifyStyles from './VerifyEmailPage.module.css'

const MIN_PASSWORD_LENGTH = 8

/**
 * Completes an account a Super Admin already created: not public self-registration, the
 * account has to already exist, passwordless and unverified, waiting on this step. Reached
 * from login, when that account's email is sent along via router state — falls back to
 * asking for it if someone lands here directly (a refresh, a bad link).
 */
export function SignUpPage() {
  const { signUp, resendOtp } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const stateEmail = (location.state as { email?: string } | null)?.email ?? ''

  const [email, setEmail] = useState(stateEmail)
  const [otp, setOtp] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [resendMessage, setResendMessage] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [resending, setResending] = useState(false)

  const isValid = email.trim() !== '' && otp.trim() !== '' && newPassword.length >= MIN_PASSWORD_LENGTH

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!isValid || submitting) return

    setSubmitting(true)
    setError(null)
    setResendMessage(null)
    try {
      const auth = await signUp({ email: email.trim(), otp: otp.trim(), newPassword })
      if (auth.token) {
        navigate(ROUTES.dashboard, { replace: true })
      }
    } catch (err) {
      const message = (err as ApiError).message
      setError(message)
      // The code they typed is dead either way — clearing it stops them from just hitting
      // "Sign up" again with the same expired/wrong code instead of requesting a new one.
      if (message === 'This code has expired. Request a new one.') {
        setOtp('')
      }
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
      title="Sign Up"
      subtitle="Enter the code we emailed you and choose your password"
      footerText="Already signed up?"
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
          label="Sign-up code"
          value={otp}
          onChange={setOtp}
          placeholder="6-digit code"
          inputMode="numeric"
          maxLength={6}
          required
        />

        <TextField
          label="Choose a password"
          type={showPassword ? 'text' : 'password'}
          value={newPassword}
          onChange={setNewPassword}
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

        {error && <ErrorMessage message={error} />}
        {resendMessage && <p className={verifyStyles.resendNotice}>{resendMessage}</p>}

        <Button type="submit" disabled={!isValid || submitting}>
          {submitting ? 'Signing up…' : 'Sign up'}
        </Button>

        <button type="button" className={verifyStyles.resendButton} onClick={handleResend} disabled={resending}>
          {resending ? 'Sending…' : "Didn't get a code? Resend"}
        </button>
      </form>
    </AuthLayout>
  )
}
