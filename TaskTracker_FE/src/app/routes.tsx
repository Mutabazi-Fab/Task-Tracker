import { Route, Routes } from 'react-router-dom'
import { DashboardPage } from '../features/dashboard/DashboardPage'
import { TaskListPage } from '../features/tasks/TaskListPage'
import { ActivityPage } from '../features/tasks/ActivityPage'
import { RequestsPage } from '../features/dashboard/RequestsPage'
import { TaskDetailPage } from '../features/taskDetail/TaskDetailPage'
import { PeopleListPage } from '../features/people/PeopleListPage'
import { PersonProfilePage } from '../features/people/PersonProfilePage'
import { TeamsListPage } from '../features/teams/TeamsListPage'
import { TeamPage } from '../features/teams/TeamPage'
import { DepartmentsListPage } from '../features/departments/DepartmentsListPage'
import { DepartmentPage } from '../features/departments/DepartmentPage'
import { SearchResultsPage } from '../features/search/SearchResultsPage'
import { LoginPage } from '../features/auth/LoginPage'
import { VerifyEmailPage } from '../features/auth/VerifyEmailPage'
import { ForgotPasswordPage } from '../features/auth/ForgotPasswordPage'
import { ResetPasswordPage } from '../features/auth/ResetPasswordPage'
import { ProtectedRoute } from '../features/auth/ProtectedRoute'
import { PublicOnlyRoute } from '../features/auth/PublicOnlyRoute'
import { AppShell } from '../components/layout/AppShell'
import { NotFoundPage } from './NotFoundPage'
import { ROUTES } from './routePaths'

// Re-exported so existing `import { ROUTES } from '.../app/routes'` keeps working — the
// actual values live in routePaths.ts (this file importing AppShell → Sidebar → ROUTES
// would otherwise be a circular import).
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

/** The actual <Route> tree. /login renders outside AppShell (no logged-in identity yet)
 *  and is wrapped in PublicOnlyRoute so an already-logged-in person skips past it. There's
 *  no public /signup route — only a Super Admin can create a new account. */
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
        path={ROUTES.verifyEmail}
        element={
          <PublicOnlyRoute>
            <VerifyEmailPage />
          </PublicOnlyRoute>
        }
      />
      <Route
        path={ROUTES.forgotPassword}
        element={
          <PublicOnlyRoute>
            <ForgotPasswordPage />
          </PublicOnlyRoute>
        }
      />
      <Route
        path={ROUTES.resetPassword}
        element={
          <PublicOnlyRoute>
            <ResetPasswordPage />
          </PublicOnlyRoute>
        }
      />

      <Route path={ROUTES.dashboard} element={protectedPage(<DashboardPage />)} />
      <Route path={ROUTES.tasks} element={protectedPage(<TaskListPage />)} />
      <Route path="/tasks/:taskId" element={protectedPage(<TaskDetailPage />)} />
      <Route path={ROUTES.activity} element={protectedPage(<ActivityPage />)} />
      <Route path={ROUTES.requests} element={protectedPage(<RequestsPage />)} />
      <Route path={ROUTES.people} element={protectedPage(<PeopleListPage />)} />
      <Route path="/people/:personId" element={protectedPage(<PersonProfilePage />)} />
      <Route path={ROUTES.teams} element={protectedPage(<TeamsListPage />)} />
      <Route path="/teams/:teamId" element={protectedPage(<TeamPage />)} />
      <Route path={ROUTES.departments} element={protectedPage(<DepartmentsListPage />)} />
      <Route path="/departments/:departmentId" element={protectedPage(<DepartmentPage />)} />
      <Route path={ROUTES.search} element={protectedPage(<SearchResultsPage />)} />
      <Route path="*" element={protectedPage(<NotFoundPage />)} />
    </Routes>
  )
}
