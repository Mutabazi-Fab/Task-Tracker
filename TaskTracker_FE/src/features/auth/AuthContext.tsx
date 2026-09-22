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
  /** Director, Executive, or Super Admin — use this rather than currentUser.role === 'DIRECTOR' directly. */
  isDirector: boolean
  /** Executive or Super Admin — the CEO's tier: Department-level task creation, task severity, the executive dashboard. */
  isExecutive: boolean
  /** Super Admin only — granting roles, deactivating accounts, department administration. */
  isSuperAdmin: boolean
  /** remember=true persists the token in localStorage; false keeps it sessionStorage-only — the "Remember me" checkbox on LoginPage. */
  login: (request: LoginRequest, remember: boolean) => Promise<void>
  /** Returns AuthResponse rather than void — a code may need another attempt. On success also logs the person in. */
  verifyEmail: (request: VerifyEmailRequest) => Promise<AuthResponse>
  resendOtp: (request: ResendOtpRequest) => Promise<void>
  /** Always resolves — the caller shows the same generic "if an account exists, a code was sent" message regardless. */
  forgotPassword: (request: ForgotPasswordRequest) => Promise<void>
  /** Same "may need another attempt" shape as verifyEmail. On success also logs the person in with their new password. */
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

/** Owns the logged-in person for the whole app — every "who's doing this" field the
 *  backend still takes explicitly is filled in from currentUser here rather than a picker.
 *  AuthResponse only carries a slim subset of fields, so right after login/verify/reset
 *  yields a real token, this fetches the full profile from GET /auth/me before considering
 *  the user "authenticated". */
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
