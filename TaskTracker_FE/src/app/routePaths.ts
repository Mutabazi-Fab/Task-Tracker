/** Every route path in one place — link to ROUTES.tasks, not "/tasks". */
export const ROUTES = {
  login: '/login',
  forgotPassword: '/forgot-password',
  totpSetup: '/totp-setup',
  totpVerify: '/totp-verify',
  dashboard: '/',
  tasks: '/tasks',
  taskDetail: (taskId: number | string) => `/tasks/${taskId}`,
  // Deliberately NOT nested under /tasks — besides the content itself likely growing
  // beyond just tasks, NavLink's prefix match would light up the Tasks item too, the same
  // way it already correctly does for /tasks/:taskId.
  activity: '/activity',
  requests: '/requests',
  people: '/people',
  personProfile: (personId: number | string) => `/people/${personId}`,
  teams: '/teams',
  team: (teamId: number | string) => `/teams/${teamId}`,
  departments: '/departments',
  department: (departmentId: number | string) => `/departments/${departmentId}`,
  incidents: '/incidents',
  incidentDetail: (incidentId: number | string) => `/incidents/${incidentId}`,
  incidentGuidance: '/incidents/guidance',
  search: '/search',
} as const
