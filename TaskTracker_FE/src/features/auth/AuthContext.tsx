import { createContext, useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { AUTH_TOKEN_STORAGE_KEY, UNAUTHORIZED_EVENT } from '../../api/axiosClient'
import type { AuthResponse, LoginRequest, ResendOtpRequest, SignupRequest, VerifyEmailRequest } from '../../types/auth.types'
import type { Person } from '../../types/person.types'
import {
  fetchCurrentPerson,
  login as loginRequest,
  logout as logoutRequest,
  resendOtp as resendOtpRequest,
  signup as signupRequest,
  verifyEmail as verifyEmailRequest,
} from './api/auth.api'

type AuthStatus = 'loading' | 'authenticated' | 'unauthenticated'

export interface AuthContextValue {
  status: AuthStatus
  currentUser: Person | null
  /** Director or Super Admin — every "Director-only" check in the UI should read this,
   *  not currentUser.role === 'DIRECTOR' directly, so Super Admin never loses access to
   *  something a Director can do. */
  isDirector: boolean
  /** Super Admin only — the handful of things exclusively theirs (granting roles,
   *  deactivating accounts, the role-change audit log). */
  isSuperAdmin: boolean
  /** remember=true persists the token in localStorage (survives closing the browser);
   *  false keeps it in sessionStorage only (gone once the tab closes) — the "Remember me"
   *  checkbox on LoginPage. */
  login: (request: LoginRequest, remember: boolean) => Promise<void>
  /**
   * Returns the raw AuthResponse rather than resolving to void — a brand-new signup comes
   * back with emailVerified=false and no token (still needs OTP verification), and the
   * caller (SignupPage) needs to see that to route to the verify-email screen instead of
   * the dashboard. Only stores a token and hydrates when one actually comes back. Always
   * remembered (no checkbox on signup) once it does.
   */
  signup: (request: SignupRequest) => Promise<AuthResponse>
  /** Same "may still need verification" shape as signup — checking a code doesn't always
   *  succeed. On success this also logs the person in. */
  verifyEmail: (request: VerifyEmailRequest) => Promise<AuthResponse>
  resendOtp: (request: ResendOtpRequest) => Promise<void>
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
 * AuthResponse (from login/signup/verify-email) only carries {token, personId, fullName,
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

  const signup = useCallback(async (request: SignupRequest): Promise<AuthResponse> => {
    const auth = await signupRequest(request)
    if (auth.token) {
      storeToken(auth.token, true)
      await hydrate()
    }
    return auth
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
      isDirector: currentUser?.role === 'DIRECTOR' || currentUser?.role === 'SUPER_ADMIN',
      isSuperAdmin: currentUser?.role === 'SUPER_ADMIN',
      login,
      signup,
      verifyEmail,
      resendOtp,
      logout,
    }),
    [status, currentUser, login, signup, verifyEmail, resendOtp, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
