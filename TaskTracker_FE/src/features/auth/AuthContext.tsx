import { createContext, useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { AUTH_TOKEN_STORAGE_KEY, UNAUTHORIZED_EVENT } from '../../api/axiosClient'
import type {
  AuthResponse,
  ForgotPasswordRequest,
  LoginRequest,
  ResendOtpRequest,
  ResetPasswordRequest,
  VerifyEmailRequest,
} from '../../types/auth.types'
import type { Person } from '../../types/person.types'
import {
  fetchCurrentPerson,
  forgotPassword as forgotPasswordRequest,
  login as loginRequest,
  logout as logoutRequest,
  resendOtp as resendOtpRequest,
  resetPassword as resetPasswordRequest,
  verifyEmail as verifyEmailRequest,
} from './api/auth.api'

type AuthStatus = 'loading' | 'authenticated' | 'unauthenticated'

export interface AuthContextValue {
  status: AuthStatus
  currentUser: Person | null
  /** Director, Executive, or Super Admin — every "Director-only" check in the UI should
   *  read this, not currentUser.role === 'DIRECTOR' directly, so Executive/Super Admin
   *  never lose access to something a Director can do. */
  isDirector: boolean
  /** Executive or Super Admin — the CEO's tier and above: org-wide (Department-level)
   *  task creation, task severity, and the org-wide executive dashboard. Super Admin sees
   *  the literal same executive view, not a separate lookalike. */
  isExecutive: boolean
  /** Super Admin only — the handful of things exclusively theirs (granting roles,
   *  deactivating accounts, the role-change audit log, department administration). */
  isSuperAdmin: boolean
  /** remember=true persists the token in localStorage (survives closing the browser);
   *  false keeps it in sessionStorage only (gone once the tab closes) — the "Remember me"
   *  checkbox on LoginPage. */
  login: (request: LoginRequest, remember: boolean) => Promise<void>
  /** Returns the raw AuthResponse rather than resolving to void — checking a code doesn't
   *  always succeed (may come back needing another attempt). On success this also logs the
   *  person in. Used by the rare legacy account still unverified (see VerifyEmailPage) —
   *  every Super-Admin-created account starts verified already. */
  verifyEmail: (request: VerifyEmailRequest) => Promise<AuthResponse>
  resendOtp: (request: ResendOtpRequest) => Promise<void>
  /** Always resolves — see ForgotPasswordRequest. The caller always shows the same generic
   *  "if an account exists, a code was sent" message regardless of what actually happened. */
  forgotPassword: (request: ForgotPasswordRequest) => Promise<void>
  /** Same "may still need to try again" shape as verifyEmail — a wrong/expired code
   *  throws. On success this also logs the person in with their new password. */
  resetPassword: (request: ResetPasswordRequest) => Promise<AuthResponse>
  logout: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)

function readStoredToken(): string | null {
  try {
    return localStorage.getItem(AUTH_TOKEN_STORAGE_KEY) ?? sessionStorage.getItem(AUTH_TOKEN_STORAGE_KEY)
  } catch {
    return null
  }
}

function storeToken(token: string, remember: boolean) {
  try {
    if (remember) {
      localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, token)
      sessionStorage.removeItem(AUTH_TOKEN_STORAGE_KEY)
    } else {
      sessionStorage.setItem(AUTH_TOKEN_STORAGE_KEY, token)
      localStorage.removeItem(AUTH_TOKEN_STORAGE_KEY)
    }
  } catch {
    // Storage unavailable — the session just won't survive a reload either way.
  }
}

function clearStoredToken() {
  try {
    localStorage.removeItem(AUTH_TOKEN_STORAGE_KEY)
    sessionStorage.removeItem(AUTH_TOKEN_STORAGE_KEY)
  } catch {
    // Nothing to clean up if it was never readable.
  }
}

/**
 * Owns the logged-in person for the whole app — every "who's doing this" field the
 * backend still takes explicitly (assignedById, changedById, authorId, ...) is filled in
 * from currentUser here rather than a picker, now that we actually know who's logged in.
 *
 * AuthResponse (from login/verify-email/reset-password) only carries {token, personId, fullName,
 * email, role, emailVerified} — not jobTitle/rank/teams — so right after any of them
 * yields a real token, this fetches the full profile from GET /auth/me before considering
 * the user "authenticated".
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>('loading')
  const [currentUser, setCurrentUser] = useState<Person | null>(null)

  const hydrate = useCallback(async () => {
    try {
      const person = await fetchCurrentPerson()
      setCurrentUser(person)
      setStatus('authenticated')
    } catch {
      clearStoredToken()
      setCurrentUser(null)
      setStatus('unauthenticated')
    }
  }, [])

  useEffect(() => {
    if (readStoredToken()) {
      void hydrate()
    } else {
      setStatus('unauthenticated')
    }
  }, [hydrate])

  useEffect(() => {
    function handleUnauthorized() {
      clearStoredToken()
      setCurrentUser(null)
      setStatus('unauthenticated')
    }
    window.addEventListener(UNAUTHORIZED_EVENT, handleUnauthorized)
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, handleUnauthorized)
  }, [])

  const login = useCallback(async (request: LoginRequest, remember: boolean) => {
    const auth = await loginRequest(request)
    // Login never comes back without a token — an unverified account is rejected with a
    // distinct error instead (see LoginPage) — but guard anyway rather than assume.
    if (auth.token) {
      storeToken(auth.token, remember)
      await hydrate()
    }
  }, [hydrate])

  const verifyEmail = useCallback(async (request: VerifyEmailRequest): Promise<AuthResponse> => {
    const auth = await verifyEmailRequest(request)
    if (auth.token) {
      storeToken(auth.token, true)
      await hydrate()
    }
    return auth
  }, [hydrate])

  const resendOtp = useCallback(async (request: ResendOtpRequest) => {
    await resendOtpRequest(request)
  }, [])

  const forgotPassword = useCallback(async (request: ForgotPasswordRequest) => {
    await forgotPasswordRequest(request)
  }, [])

  const resetPassword = useCallback(async (request: ResetPasswordRequest): Promise<AuthResponse> => {
    const auth = await resetPasswordRequest(request)
    if (auth.token) {
      storeToken(auth.token, true)
      await hydrate()
    }
    return auth
  }, [hydrate])

  const logout = useCallback(() => {
    // Best-effort — JWT is stateless, so there's nothing server-side to wait on.
    void logoutRequest().catch(() => {
      // Ignored on purpose: even if this call fails, the client still discards its token.
    })
    clearStoredToken()
    setCurrentUser(null)
    setStatus('unauthenticated')
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({
      status,
      currentUser,
      isDirector:
        currentUser?.role === 'DIRECTOR' ||
        currentUser?.role === 'EXECUTIVE' ||
        currentUser?.role === 'SUPER_ADMIN',
      isExecutive: currentUser?.role === 'EXECUTIVE' || currentUser?.role === 'SUPER_ADMIN',
      isSuperAdmin: currentUser?.role === 'SUPER_ADMIN',
      login,
      verifyEmail,
      resendOtp,
      forgotPassword,
      resetPassword,
      logout,
    }),
    [status, currentUser, login, verifyEmail, resendOtp, forgotPassword, resetPassword, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
