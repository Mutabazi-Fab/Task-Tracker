import { Route, Routes } from 'react-router-dom'
import { DashboardPage } from '../features/dashboard/DashboardPage'
import { TaskListPage } from '../features/tasks/TaskListPage'
import { TaskDetailPage } from '../features/taskDetail/TaskDetailPage'
import { PeopleListPage } from '../features/people/PeopleListPage'
import { PersonProfilePage } from '../features/people/PersonProfilePage'
import { RoleChangeActivityPage } from '../features/people/RoleChangeActivityPage'
import { TeamsListPage } from '../features/teams/TeamsListPage'
import { TeamPage } from '../features/teams/TeamPage'
import { SearchResultsPage } from '../features/search/SearchResultsPage'
import { LoginPage } from '../features/auth/LoginPage'
import { SignupPage } from '../features/auth/SignupPage'
import { VerifyEmailPage } from '../features/auth/VerifyEmailPage'
import { ProtectedRoute } from '../features/auth/ProtectedRoute'
import { PublicOnlyRoute } from '../features/auth/PublicOnlyRoute'
import { AppShell } from '../components/layout/AppShell'
import { NotFoundPage } from './NotFoundPage'
import { ROUTES } from './routePaths'

// Re-exported so every existing `import { ROUTES } from '.../app/routes'` across the app
// keeps working unchanged — the actual values live in routePaths.ts now (see the comment
// there for why: this file importing AppShell, which imports Sidebar, which needs ROUTES,
// is a circular import that crashed at runtime when ROUTES was defined here directly).
export { ROUTES }

/** Every logged-in route rendered inside AppShell (sidebar + top bar) and gated by
 *  ProtectedRoute — pulled into one helper so that isn't repeated per route. */
function protectedPage(page: React.ReactNode) {
  return (
    <ProtectedRoute>
      <AppShell>{page}</AppShell>
    </ProtectedRoute>
  )
}

/**
 * The actual <Route> tree. Pages not built yet fall through to the "*"
 * NotFoundPage rather than a fake stub — Tasks/People/Teams/Search land
 * here as their features are built.
 *
 * /login and /signup render outside AppShell (no sidebar, no search bar —
 * there's no logged-in identity yet to build those around) and are wrapped
 * in PublicOnlyRoute instead, so an already-logged-in person skips straight
 * past them.
 */
export function AppRoutes() {
  return (
    <Routes>
      <Route
        path={ROUTES.login}
        element={
          <PublicOnlyRoute>
            <LoginPage />
          </PublicOnlyRoute>
        }
      />
      <Route
        path={ROUTES.signup}
        element={
          <PublicOnlyRoute>
            <SignupPage />
          </PublicOnlyRoute>
        }
      />
      <Route
        path={ROUTES.verifyEmail}
        element={
          <PublicOnlyRoute>
            <VerifyEmailPage />
          </PublicOnlyRoute>
        }
      />

      <Route path={ROUTES.dashboard} element={protectedPage(<DashboardPage />)} />
      <Route path={ROUTES.tasks} element={protectedPage(<TaskListPage />)} />
      <Route path="/tasks/:taskId" element={protectedPage(<TaskDetailPage />)} />
      <Route path={ROUTES.people} element={protectedPage(<PeopleListPage />)} />
      <Route path="/people/:personId" element={protectedPage(<PersonProfilePage />)} />
      <Route path={ROUTES.teams} element={protectedPage(<TeamsListPage />)} />
      <Route path="/teams/:teamId" element={protectedPage(<TeamPage />)} />
      <Route path={ROUTES.search} element={protectedPage(<SearchResultsPage />)} />
      <Route path={ROUTES.roleChanges} element={protectedPage(<RoleChangeActivityPage />)} />
      <Route path="*" element={protectedPage(<NotFoundPage />)} />
    </Routes>
  )
}
