import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { Button } from '../../components/ui/Button'
import { ErrorMessage } from '../../components/ui/ErrorMessage'
import { Icon } from '../../components/ui/Icon'
import { ROUTES } from '../../app/routes'
import { AuthField } from './components/AuthField'
import { AuthLayout } from './components/AuthLayout'
import { useAuth } from './useAuth'
import type { ApiError } from '../../api/axiosClient'
import styles from './components/AuthLayout.module.css'
import loginStyles from './LoginPage.module.css'

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [remember, setRemember] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const isValid = email.trim() !== '' && password !== ''

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!isValid || submitting) return

    setSubmitting(true)
    setError(null)
    try {
      await login({ email: email.trim(), password }, remember)
      const redirectTo = (location.state as { from?: string } | null)?.from ?? ROUTES.dashboard
      navigate(redirectTo, { replace: true })
    } catch (err) {
      const message = (err as ApiError).message
      // Matched by text, not a structured code — the backend distinguishes this from a
      // generic bad-credentials rejection with a specific message precisely so the
      // frontend can route to verification instead of just showing an error.
      if (message === 'Please verify your email before logging in.') {
        navigate(ROUTES.verifyEmail, { state: { email: email.trim() } })
        return
      }
      setError(message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthLayout
      title="Welcome Back"
      subtitle="Sign in to keep tracking your team's progress"
      footerText="Don't have an account?"
      footerLinkTo={ROUTES.signup}
      footerLinkLabel="Create one"
    >
      <form className={styles.form} onSubmit={handleSubmit}>
        <AuthField
          label="Email Address"
          type="email"
          icon="mail"
          value={email}
          onChange={setEmail}
          placeholder="Enter your email"
          autoComplete="email"
          required
        />
        <AuthField
          label="Password"
          type={showPassword ? 'text' : 'password'}
          icon="lock"
          value={password}
          onChange={setPassword}
          placeholder="Enter your password"
          autoComplete="current-password"
          required
          trailing={
            <button
              type="button"
              className={loginStyles.eyeButton}
              onClick={() => setShowPassword((v) => !v)}
              aria-label={showPassword ? 'Hide password' : 'Show password'}
            >
              <Icon name={showPassword ? 'eyeOff' : 'eye'} size={16} />
            </button>
          }
        />

        <div className={loginStyles.rememberRow}>
          <label className={loginStyles.rememberCheckbox}>
            <input type="checkbox" checked={remember} onChange={(e) => setRemember(e.target.checked)} />
            Remember me
          </label>
          <Link to={ROUTES.forgotPassword} className={loginStyles.forgotLink}>
            Forgot password?
          </Link>
        </div>

        {error && <ErrorMessage message={error} />}

        <Button type="submit" disabled={!isValid || submitting}>
          {submitting ? 'Signing in…' : 'Sign in'}
        </Button>
      </form>
    </AuthLayout>
  )
}
