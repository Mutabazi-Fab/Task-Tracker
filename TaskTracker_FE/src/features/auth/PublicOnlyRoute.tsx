import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { LoadingSpinner } from '../../components/ui/LoadingSpinner'
import { ROUTES } from '../../app/routes'
import { useAuth } from './useAuth'

/** Wraps /login and /signup — an already-logged-in person is sent straight to the
 *  dashboard instead of seeing the login form again. */
export function PublicOnlyRoute({ children }: { children: ReactNode }) {
  const { status } = useAuth()

  if (status === 'loading') {
    return <LoadingSpinner label="Loading your session" />
  }

  if (status === 'authenticated') {
    return <Navigate to={ROUTES.dashboard} replace />
  }

  return <>{children}</>
}
