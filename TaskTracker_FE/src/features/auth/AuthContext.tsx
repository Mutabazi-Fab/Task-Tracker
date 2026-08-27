import { createContext, useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { AUTH_TOKEN_STORAGE_KEY, UNAUTHORIZED_EVENT } from '../../api/axiosClient'
import type { LoginRequest, SignupRequest } from '../../types/auth.types'
import type { Person } from '../../types/person.types'
import { fetchCurrentPerson, login as loginRequest, logout as logoutRequest, signup as signupRequest } from './api/auth.api'

type AuthStatus = 'loading' | 'authenticated' | 'unauthenticated'

export interface AuthContextValue {
  status: AuthStatus
  currentUser: Person | null
  isDirector: boolean
  /** remember=true persists the token in localStorage (survives closing the browser);
   *  false keeps it in sessionStorage only (gone once the tab closes) — the "Remember me"
   *  checkbox on LoginPage. */
  login: (request: LoginRequest, remember: boolean) => Promise<void>
  /** Always remembered — there's no checkbox on signup, and someone who just created an
   *  account has no reason to expect it to sign them out the moment they close the tab. */
  signup: (request: SignupRequest) => Promise<void>
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
 * AuthResponse (from login/signup) only carries {token, personId, fullName, email, role} —
 * not jobTitle/rank/teams — so right after either one succeeds, this fetches the full
 * profile from GET /auth/me before considering the user "authenticated".
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
    storeToken(auth.token, remember)
    await hydrate()
  }, [hydrate])

  const signup = useCallback(async (request: SignupRequest) => {
    const auth = await signupRequest(request)
    storeToken(auth.token, true)
    await hydrate()
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
      isDirector: currentUser?.role === 'DIRECTOR',
      login,
      signup,
      logout,
    }),
    [status, currentUser, login, signup, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
