/**
 * Every backend URL as one constant or path-builder function. No component,
 * hook or *.api.ts file should ever write a literal "/api/v1/..." string —
 * it imports it from here.
 */

const BASE = '/api/v1'

export const endpoints = {
  auth: {
    signup: () => `${BASE}/auth/signup`,
    login: () => `${BASE}/auth/login`,
    logout: () => `${BASE}/auth/logout`,
    me: () => `${BASE}/auth/me`,
    verifyEmail: () => `${BASE}/auth/verify-email`,
    resendOtp: () => `${BASE}/auth/resend-otp`,
    forgotPassword: () => `${BASE}/auth/forgot-password`,
    resetPassword: () => `${BASE}/auth/reset-password`,
  },
  tasks: {
    list: () => `${BASE}/tasks`,
    create: () => `${BASE}/tasks`,
    subtasks: (parentTaskId: number | string) => `${BASE}/tasks/${parentTaskId}/subtasks`,
    detail: (id: number | string) => `${BASE}/tasks/${id}`,
    byCode: (taskCode: string) => `${BASE}/tasks/code/${taskCode}`,
    search: () => `${BASE}/tasks/search`,
    update: (id: number | string) => `${BASE}/tasks/${id}`,
    remove: (id: number | string) => `${BASE}/tasks/${id}`,
    comments: (id: number | string) => `${BASE}/tasks/${id}/comments`,
    addComment: (id: number | string) => `${BASE}/tasks/${id}/comments`,
    reassign: (id: number | string) => `${BASE}/tasks/${id}/reassign`,
    reassignments: (id: number | string) => `${BASE}/tasks/${id}/reassignments`,
    progressTimeline: (id: number | string) => `${BASE}/tasks/${id}/progress-timeline`,
    activity: () => `${BASE}/tasks/activity`,
    deadline: (id: number | string) => `${BASE}/tasks/${id}/deadline`,
    deadlineExtensions: (id: number | string) => `${BASE}/tasks/${id}/deadline-extensions`,
    decideDeadlineExtension: (id: number | string, extensionId: number | string) =>
      `${BASE}/tasks/${id}/deadline-extensions/${extensionId}`,
    forwardDeadlineExtension: (id: number | string, extensionId: number | string) =>
      `${BASE}/tasks/${id}/deadline-extensions/${extensionId}/forward`,
    pendingDeadlineExtensions: () => `${BASE}/tasks/deadline-extensions/pending`,
    pin: (id: number | string) => `${BASE}/tasks/${id}/pin`,
    discussionComments: (id: number | string) => `${BASE}/tasks/${id}/discussion-comments`,
    documents: (id: number | string) => `${BASE}/tasks/${id}/documents`,
    document: (id: number | string, documentId: number | string) => `${BASE}/tasks/${id}/documents/${documentId}`,
    documentDownload: (id: number | string, documentId: number | string) =>
      `${BASE}/tasks/${id}/documents/${documentId}/download`,
  },
  people: {
    list: () => `${BASE}/people`,
    create: () => `${BASE}/people`,
    detail: (id: number | string) => `${BASE}/people/${id}`,
    update: (id: number | string) => `${BASE}/people/${id}`,
    remove: (id: number | string) => `${BASE}/people/${id}`,
    statistics: (id: number | string) => `${BASE}/people/${id}/statistics`,
    tasks: (id: number | string) => `${BASE}/people/${id}/tasks`,
    changeRole: (id: number | string) => `${BASE}/people/${id}/role`,
    setActive: (id: number | string) => `${BASE}/people/${id}/active`,
    roleChanges: () => `${BASE}/people/role-changes`,
    accountStatusChanges: () => `${BASE}/people/account-status-changes`,
    sendPasswordReset: (id: number | string) => `${BASE}/people/${id}/send-password-reset`,
    // No assignTeam — team membership is exclusively managed through the teams.* endpoints
    // below now, since a person can belong to multiple teams at once.
  },
  teams: {
    list: () => `${BASE}/teams`,
    create: () => `${BASE}/teams`,
    detail: (id: number | string) => `${BASE}/teams/${id}`,
    update: (id: number | string) => `${BASE}/teams/${id}`,
    remove: (id: number | string) => `${BASE}/teams/${id}`,
    statistics: (id: number | string) => `${BASE}/teams/${id}/statistics`,
    tasks: (id: number | string) => `${BASE}/teams/${id}/tasks`,
    members: (id: number | string) => `${BASE}/teams/${id}/members`,
    setLeader: (id: number | string, personId: number | string) =>
      `${BASE}/teams/${id}/leader/${personId}`,
    // personId goes in the request body (AddTeamMemberRequest), not the URL.
    addMember: (id: number | string) => `${BASE}/teams/${id}/members`,
    removeMember: (id: number | string, personId: number | string) =>
      `${BASE}/teams/${id}/members/${personId}`,
    membershipHistory: (id: number | string) => `${BASE}/teams/${id}/membership-history`,
    activity: () => `${BASE}/teams/activity`,
  },
  departments: {
    list: () => `${BASE}/departments`,
    create: () => `${BASE}/departments`,
    detail: (id: number | string) => `${BASE}/departments/${id}`,
    rename: (id: number | string) => `${BASE}/departments/${id}`,
    changeHead: (id: number | string) => `${BASE}/departments/${id}/head`,
    remove: (id: number | string) => `${BASE}/departments/${id}`,
    activity: () => `${BASE}/departments/activity`,
  },
  notifications: {
    list: () => `${BASE}/notifications`,
    markRead: (id: number | string) => `${BASE}/notifications/${id}/read`,
    unreadCount: () => `${BASE}/notifications/unread-count`,
    unreadCountsByType: () => `${BASE}/notifications/unread-counts-by-type`,
    markCategoryRead: () => `${BASE}/notifications/mark-category-read`,
  },
  dashboard: {
    overview: () => `${BASE}/dashboard/overview`,
    statusMix: () => `${BASE}/dashboard/status-mix`,
    progressOverTime: () => `${BASE}/dashboard/progress-over-time`,
    teamLeaderboard: () => `${BASE}/dashboard/team-leaderboard`,
    peopleSummary: () => `${BASE}/dashboard/people-summary`,
    search: () => `${BASE}/dashboard/search`,
    directorTasks: () => `${BASE}/dashboard/director/tasks`,
    executiveTasks: () => `${BASE}/dashboard/executive/tasks`,
    executiveDepartmentHealth: () => `${BASE}/dashboard/executive/department-health`,
    executiveKpis: () => `${BASE}/dashboard/executive/kpis`,
  },
} as const
