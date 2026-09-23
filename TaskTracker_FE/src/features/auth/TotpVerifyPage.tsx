import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { Button } from '../../components/ui/Button'
import { ErrorMessage } from '../../components/ui/ErrorMessage'
import { TextField } from '../../components/ui/TextField'
import { ROUTES } from '../../app/routes'
import { AuthLayout } from './components/AuthLayout'
import { routeAfterAuthResponse } from './routeAfterAuthResponse'
import { useAuth } from './useAuth'
import type { ApiError } from '../../api/axiosClient'
import styles from './components/AuthLayout.module.css'

interface TotpVerifyState {
  pendingAuthToken?: string
  remember?: boolean
}

/**
 * Reached from login/reset-password when an already-enrolled account still needs its TOTP
 * code — pendingAuthToken and the original "remember me" choice arrive via router state.
 * There's no resend here: unlike an emailed code, a TOTP code is generated locally on the
 * person's own phone every 30 seconds, so "didn't get one" isn't a real failure mode —
 * checking the phone's clock is. Falls back to login if reached without a token (a
 * refresh, a bad link), since there's nothing this page can do without one.
 */
export function TotpVerifyPage() {
  const { verifyTotp } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const state = (location.state as TotpVerifyState | null) ?? {}
  const pendingAuthToken = state.pendingAuthToken ?? ''
  const remember = state.remember ?? false

  const [code, setCode] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (!pendingAuthToken) {
      navigate(ROUTES.login, { replace: true })
    }
  }, [pendingAuthToken, navigate])

  const isValid = code.trim() !== ''

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!isValid || submitting) return

    setSubmitting(true)
    setError(null)
    try {
      const auth = await verifyTotp({ pendingAuthToken, code: code.trim() }, remember)
      routeAfterAuthResponse(navigate, auth, remember)
    } catch (err) {
      setError((err as ApiError).message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthLayout
      title="Two-Factor Authentication"
      subtitle="Enter the 6-digit code from your authenticator app"
    >
      <form className={styles.form} onSubmit={handleSubmit}>
        <TextField
          label="Authentication code"
          value={code}
          onChange={setCode}
          placeholder="6-digit code, or a recovery code"
          inputMode="numeric"
          maxLength={9}
          autoFocus
          required
        />

        {error && <ErrorMessage message={error} />}

        <Button type="submit" disabled={!isValid || submitting}>
          {submitting ? 'Verifying…' : 'Verify'}
        </Button>
      </form>
    </AuthLayout>
  )
}
