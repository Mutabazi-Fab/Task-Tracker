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
  login: (request: LoginRequest) => Promise<void>
  signup: (request: SignupRequest) => Promise<void>
  logout: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)

function readStoredToken(): string | null {
  try {
    return localStorage.getItem(AUTH_TOKEN_STORAGE_KEY)
  } catch {
    return null
  }
}

function storeToken(token: string) {
  try {
    localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, token)
  } catch {
    // localStorage unavailable — the session just won't survive a reload.
  }
}

function clearStoredToken() {
  try {
    localStorage.removeItem(AUTH_TOKEN_STORAGE_KEY)
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

  const login = useCallback(async (request: LoginRequest) => {
    const auth = await loginRequest(request)
    storeToken(auth.token)
    await hydrate()
  }, [hydrate])

  const signup = useCallback(async (request: SignupRequest) => {
    const auth = await signupRequest(request)
    storeToken(auth.token)
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
