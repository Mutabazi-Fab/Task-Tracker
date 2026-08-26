import { Route, Routes } from 'react-router-dom'
import { DashboardPage } from '../features/dashboard/DashboardPage'
import { TaskListPage } from '../features/tasks/TaskListPage'
import { TaskDetailPage } from '../features/taskDetail/TaskDetailPage'
import { PeopleListPage } from '../features/people/PeopleListPage'
import { PersonProfilePage } from '../features/people/PersonProfilePage'
import { TeamsListPage } from '../features/teams/TeamsListPage'
import { TeamPage } from '../features/teams/TeamPage'
import { SearchResultsPage } from '../features/search/SearchResultsPage'
import { NotFoundPage } from './NotFoundPage'

/**
 * Every route path in one place. Nothing else in the app should write a
 * literal path string — link to ROUTES.tasks, not "/tasks".
 */
export const ROUTES = {
  dashboard: '/',
  tasks: '/tasks',
  taskDetail: (taskId: number | string) => `/tasks/${taskId}`,
  people: '/people',
  personProfile: (personId: number | string) => `/people/${personId}`,
  teams: '/teams',
  team: (teamId: number | string) => `/teams/${teamId}`,
  search: '/search',
} as const

/**
 * The actual <Route> tree. Pages not built yet fall through to the "*"
 * NotFoundPage rather than a fake stub — Tasks/People/Teams/Search land
 * here as their features are built.
 */
export function AppRoutes() {
  return (
    <Routes>
      <Route path={ROUTES.dashboard} element={<DashboardPage />} />
      <Route path={ROUTES.tasks} element={<TaskListPage />} />
      <Route path="/tasks/:taskId" element={<TaskDetailPage />} />
      <Route path={ROUTES.people} element={<PeopleListPage />} />
      <Route path="/people/:personId" element={<PersonProfilePage />} />
      <Route path={ROUTES.teams} element={<TeamsListPage />} />
      <Route path="/teams/:teamId" element={<TeamPage />} />
      <Route path={ROUTES.search} element={<SearchResultsPage />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
