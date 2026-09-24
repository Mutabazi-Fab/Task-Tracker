import { useEffect, useState } from 'react'
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
import setupStyles from './TotpSetupPage.module.css'

interface TotpSetupState {
  pendingAuthToken?: string
  totpSecret?: string
  totpQrCodeDataUri?: string
  remember?: boolean
}

/** Reached from login/reset-password on an account that requires TOTP but has never completed
 *  enrollment — pendingAuthToken, totpSecret, and totpQrCodeDataUri all arrive via router state (login
 *  already generated and stored the secret; this page never sees or handles it directly). */
export function TotpSetupPage() {
  const { confirmTotpSetup } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const state = (location.state as TotpSetupState | null) ?? {}
  const pendingAuthToken = state.pendingAuthToken ?? ''
  const totpSecret = state.totpSecret ?? ''
  const totpQrCodeDataUri = state.totpQrCodeDataUri ?? ''
  const remember = state.remember ?? false

  const [code, setCode] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [recoveryCodes, setRecoveryCodes] = useState<string[] | null>(null)
  const [copied, setCopied] = useState(false)

  useEffect(() => {
    if (!pendingAuthToken || !totpQrCodeDataUri) {
      navigate(ROUTES.login, { replace: true })
    }
  }, [pendingAuthToken, totpQrCodeDataUri, navigate])

  const isValid = code.trim() !== ''

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!isValid || submitting) return

    setSubmitting(true)
    setError(null)
    try {
      const auth = await confirmTotpSetup({ pendingAuthToken, code: code.trim() }, remember)
      setRecoveryCodes(auth.recoveryCodes ?? [])
    } catch (err) {
      setError((err as ApiError).message)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleCopyRecoveryCodes() {
    if (!recoveryCodes) return
    try {
      await navigator.clipboard.writeText(recoveryCodes.join('\n'))
      setCopied(true)
    } catch {
      // Clipboard unavailable — the codes are still visible on screen to copy by hand.
    }
  }

  function handleContinue() {
    navigate(ROUTES.dashboard, { replace: true })
  }

  if (recoveryCodes) {
    return (
      <AuthLayout
        title="Save Your Recovery Codes"
        subtitle="Each code works once, in place of a 6-digit code, if you ever lose this device"
      >
        <div className={styles.form}>
          <div className={setupStyles.recoveryGrid}>
            {recoveryCodes.map((rc) => (
              <span key={rc} className={setupStyles.recoveryCode}>{rc}</span>
            ))}
          </div>

          <button type="button" className={setupStyles.copyButton} onClick={handleCopyRecoveryCodes}>
            <Icon name="copy" size={16} />
            {copied ? 'Copied' : 'Copy all'}
          </button>

          <p className={setupStyles.warning}>
            This is the only time these will be shown. Store them somewhere safe — a password
            manager, or printed and kept with your other records.
          </p>

          <Button onClick={handleContinue}>I've saved these — continue</Button>
        </div>
      </AuthLayout>
    )
  }

  return (
    <AuthLayout
      title="Set Up Two-Factor Authentication"
      subtitle="Scan this with Google Authenticator or any TOTP app"
    >
      <div className={styles.form}>
        <div className={setupStyles.qrWrap}>
          <img src={totpQrCodeDataUri} alt="TOTP enrollment QR code" className={setupStyles.qrImage} />
        </div>

        <p className={setupStyles.manualEntry}>
          Can't scan it? Enter this key manually: <code className={setupStyles.secret}>{totpSecret}</code>
        </p>

        <form onSubmit={handleSubmit} className={styles.form}>
          <TextField
            label="6-digit code"
            value={code}
            onChange={setCode}
            placeholder="From your authenticator app"
            inputMode="numeric"
            maxLength={6}
            autoFocus
            required
          />

          {error && <ErrorMessage message={error} />}

          <Button type="submit" disabled={!isValid || submitting}>
            {submitting ? 'Confirming…' : 'Confirm and finish setup'}
          </Button>
        </form>
      </div>
    </AuthLayout>
  )
}
