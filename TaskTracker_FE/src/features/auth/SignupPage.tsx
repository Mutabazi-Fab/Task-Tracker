import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button } from '../../components/ui/Button'
import { ErrorMessage } from '../../components/ui/ErrorMessage'
import { TextField } from '../../components/ui/TextField'
import { ROUTES } from '../../app/routes'
import { AuthLayout } from './components/AuthLayout'
import { useAuth } from './useAuth'
import type { ApiError } from '../../api/axiosClient'
import styles from './components/AuthLayout.module.css'

/** No role picker here on purpose — self-service signup always creates a MEMBER
 *  (see the backend's SignupRequest doc comment); Director accounts are provisioned
 *  out of band, there's no self-escalation path. */
export function SignupPage() {
  const { signup } = useAuth()
  const navigate = useNavigate()

  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [jobTitle, setJobTitle] = useState('')
  const [rank, setRank] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const isValid =
    fullName.trim() !== '' && email.trim() !== '' && password.length >= 8 && jobTitle.trim() !== ''

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!isValid || submitting) return

    setSubmitting(true)
    setError(null)
    try {
      await signup({
        fullName: fullName.trim(),
        email: email.trim(),
        password,
        jobTitle: jobTitle.trim(),
        rank: rank.trim() || undefined,
      })
      navigate(ROUTES.dashboard, { replace: true })
    } catch (err) {
      setError((err as ApiError).message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthLayout
      title="Create Your Account"
      subtitle="Join Throughline to start tracking your work"
      footerText="Already have an account?"
      footerLinkTo={ROUTES.login}
      footerLinkLabel="Sign in"
    >
      <form className={styles.form} onSubmit={handleSubmit}>
        <TextField label="Full name" value={fullName} onChange={setFullName} placeholder="Your full name" required />
        <TextField
          label="Email"
          type="email"
          value={email}
          onChange={setEmail}
          placeholder="you@example.com"
          autoComplete="email"
          required
        />
        <TextField
          label="Password"
          type="password"
          value={password}
          onChange={setPassword}
          placeholder="At least 8 characters"
          autoComplete="new-password"
          required
        />
        <TextField label="Job title" value={jobTitle} onChange={setJobTitle} placeholder="e.g. Backend Engineer" required />
        <TextField label="Rank (optional)" value={rank} onChange={setRank} placeholder="e.g. Captain" />

        {error && <ErrorMessage message={error} />}

        <Button type="submit" disabled={!isValid || submitting}>
          {submitting ? 'Creating account…' : 'Create account'}
        </Button>
      </form>
    </AuthLayout>
  )
}
