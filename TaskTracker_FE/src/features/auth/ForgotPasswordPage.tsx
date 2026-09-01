import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button } from '../../components/ui/Button'
import { TextField } from '../../components/ui/TextField'
import { ROUTES } from '../../app/routes'
import { AuthLayout } from './components/AuthLayout'
import { useAuth } from './useAuth'
import styles from './components/AuthLayout.module.css'
import verifyStyles from './VerifyEmailPage.module.css'

/**
 * Deliberately can't fail from the user's point of view — forgotPassword always resolves
 * (see ForgotPasswordRequest), so this always moves on to the reset-code screen with the
 * same generic message, whether or not the email is actually registered. That's what keeps
 * this endpoint from being usable to check who has an account.
 */
export function ForgotPasswordPage() {
  const { forgotPassword } = useAuth()
  const navigate = useNavigate()

  const [email, setEmail] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const isValid = email.trim() !== ''

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!isValid || submitting) return

    setSubmitting(true)
    try {
      await forgotPassword({ email: email.trim() })
      navigate(ROUTES.resetPassword, { state: { email: email.trim() } })
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthLayout
      title="Forgot Password"
      subtitle="Enter your email and we'll send you a reset code"
      footerText="Remembered it?"
      footerLinkTo={ROUTES.login}
      footerLinkLabel="Sign in"
    >
      <form className={styles.form} onSubmit={handleSubmit}>
        <TextField
          label="Email address"
          type="email"
          value={email}
          onChange={setEmail}
          placeholder="you@example.com"
          required
        />
        <p className={verifyStyles.emailNotice}>
          If an account with that email exists, we'll send a reset code to it.
        </p>

        <Button type="submit" disabled={!isValid || submitting}>
          {submitting ? 'Sending…' : 'Send reset code'}
        </Button>
      </form>
    </AuthLayout>
  )
}
