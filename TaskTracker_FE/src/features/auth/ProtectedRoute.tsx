import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { LoadingSpinner } from '../../components/ui/LoadingSpinner'
import { ROUTES } from '../../app/routes'
import { useAuth } from './useAuth'

/** Wraps every route that needs a logged-in person. */
export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { status } = useAuth()
  const location = useLocation()

  if (status === 'loading') {
    return <LoadingSpinner label="Loading your session" />
  }

  if (status === 'unauthenticated') {
    return <Navigate to={ROUTES.login} replace state={{ from: location.pathname }} />
  }

  return <>{children}</>
}
