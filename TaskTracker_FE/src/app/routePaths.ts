/**
 * Every route path in one place. Nothing else in the app should write a
 * literal path string — link to ROUTES.tasks, not "/tasks".
 *
 * Deliberately its own module with zero imports: routes.tsx (which defines
 * AppRoutes) imports AppShell, which imports Sidebar, which needs ROUTES —
 * if ROUTES lived in routes.tsx itself, that would be a circular import
 * (routes.tsx -> AppShell -> Sidebar -> routes.tsx) that crashes at runtime
 * with "Cannot access 'ROUTES' before initialization", since Sidebar reads
 * it at module-evaluation time (building NAV_ITEMS), before routes.tsx has
 * finished running its own top-level code.
 */
export const ROUTES = {
  login: '/login',
  signup: '/signup',
  verifyEmail: '/verify-email',
  forgotPassword: '/forgot-password',
  resetPassword: '/reset-password',
  dashboard: '/',
  tasks: '/tasks',
  taskDetail: (taskId: number | string) => `/tasks/${taskId}`,
  people: '/people',
  personProfile: (personId: number | string) => `/people/${personId}`,
  teams: '/teams',
  team: (teamId: number | string) => `/teams/${teamId}`,
  search: '/search',
  roleChanges: '/admin/role-changes',
} as const
