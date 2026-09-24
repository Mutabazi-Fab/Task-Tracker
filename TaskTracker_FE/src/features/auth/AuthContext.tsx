import { createContext, useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { AUTH_TOKEN_STORAGE_KEY, UNAUTHORIZED_EVENT } from '../../api/axiosClient'
import type {
  AuthResponse,
  CheckEmailResponse,
  LoginRequest,
  PasswordResetEmailRequest,
  PasswordResetRequestOutcome,
  TotpConfirmRequest,
  TotpVerifyRequest,
} from '../../types/auth.types'
import type { Person } from '../../types/person.types'
import {
  checkEmailForPasswordReset as checkEmailForPasswordResetRequest,
  confirmTotpSetup as confirmTotpSetupRequest,
  createPasswordResetRequest as createPasswordResetRequestRequest,
  fetchCurrentPerson,
  login as loginRequest,
  logout as logoutRequest,
  verifyTotp as verifyTotpRequest,
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
  /** remember=true persists the token in localStorage; false keeps it sessionStorage-only — the "Remember
   *  me" checkbox on LoginPage. */
  login: (request: LoginRequest, remember: boolean) => Promise<AuthResponse>
  /** Completes TOTP enrollment. */
  confirmTotpSetup: (request: TotpConfirmRequest, remember: boolean) => Promise<AuthResponse>
  /** Completes a login for an already-enrolled account. Same remember-carries-through shape
   *  as confirmTotpSetup above. */
  verifyTotp: (request: TotpVerifyRequest, remember: boolean) => Promise<AuthResponse>
  /** Whether an account exists for this email — the backend deliberately tells the truth
   *  here (rate-limited, 5 checks per email per 15 minutes) rather than staying silent. */
  checkEmailForPasswordReset: (request: PasswordResetEmailRequest) => Promise<CheckEmailResponse>
  /** Creates a password-reset request for the Super Admin to see, or reports that one is already pending. */
  createPasswordResetRequest: (request: PasswordResetEmailRequest) => Promise<PasswordResetRequestOutcome>
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

/** Owns the logged-in person for the whole app — every "who's doing this" field the backend still takes
 *  explicitly is filled in from currentUser here rather than a picker. */
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

  const login = useCallback(async (request: LoginRequest, remember: boolean): Promise<AuthResponse> => {
    const auth = await loginRequest(request)
    // A TOTP-enabled account comes back with status TOTP_SETUP_REQUIRED/TOTP_REQUIRED and
    // no token yet — LoginPage routes to enrollment/challenge in that case instead.
    if (auth.token) {
      storeToken(auth.token, remember)
      await hydrate()
    }
    return auth
  }, [hydrate])

  const confirmTotpSetup = useCallback(async (request: TotpConfirmRequest, remember: boolean): Promise<AuthResponse> => {
    const auth = await confirmTotpSetupRequest(request)
    if (auth.token) {
      storeToken(auth.token, remember)
      await hydrate()
    }
    return auth
  }, [hydrate])

  const verifyTotp = useCallback(async (request: TotpVerifyRequest, remember: boolean): Promise<AuthResponse> => {
    const auth = await verifyTotpRequest(request)
    if (auth.token) {
      storeToken(auth.token, remember)
      await hydrate()
    }
    return auth
  }, [hydrate])

  const checkEmailForPasswordReset = useCallback(async (request: PasswordResetEmailRequest) => {
    return checkEmailForPasswordResetRequest(request)
  }, [])

  const createPasswordResetRequest = useCallback(async (request: PasswordResetEmailRequest) => {
    return createPasswordResetRequestRequest(request)
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
      isDirector:
        currentUser?.role === 'DIRECTOR' ||
        currentUser?.role === 'EXECUTIVE' ||
        currentUser?.role === 'SUPER_ADMIN',
      isExecutive: currentUser?.role === 'EXECUTIVE' || currentUser?.role === 'SUPER_ADMIN',
      isSuperAdmin: currentUser?.role === 'SUPER_ADMIN',
      login,
      confirmTotpSetup,
      verifyTotp,
      checkEmailForPasswordReset,
      createPasswordResetRequest,
      logout,
    }),
    [
      status, currentUser, login, confirmTotpSetup, verifyTotp,
      checkEmailForPasswordReset, createPasswordResetRequest, logout,
    ],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
