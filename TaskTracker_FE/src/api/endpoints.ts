/**
 * Every backend URL as one constant or path-builder function. No component,
 * hook or *.api.ts file should ever write a literal "/api/v1/..." string —
 * it imports it from here.
 */

const BASE = '/api/v1'

export const endpoints = {
  tasks: {
    list: () => `${BASE}/tasks`,
    create: () => `${BASE}/tasks`,
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
  },
  people: {
    list: () => `${BASE}/people`,
    create: () => `${BASE}/people`,
    detail: (id: number | string) => `${BASE}/people/${id}`,
    update: (id: number | string) => `${BASE}/people/${id}`,
    remove: (id: number | string) => `${BASE}/people/${id}`,
    statistics: (id: number | string) => `${BASE}/people/${id}/statistics`,
    tasks: (id: number | string) => `${BASE}/people/${id}/tasks`,
    assignTeam: (id: number | string, teamId: number | string) =>
      `${BASE}/people/${id}/team/${teamId}`,
  },
  teams: {
    list: () => `${BASE}/teams`,
    create: () => `${BASE}/teams`,
    detail: (id: number | string) => `${BASE}/teams/${id}`,
    update: (id: number | string) => `${BASE}/teams/${id}`,
    remove: (id: number | string) => `${BASE}/teams/${id}`,
    statistics: (id: number | string) => `${BASE}/teams/${id}/statistics`,
    tasks: (id: number | string) => `${BASE}/teams/${id}/tasks`,
    setLeader: (id: number | string, personId: number | string) =>
      `${BASE}/teams/${id}/leader/${personId}`,
    addMember: (id: number | string, personId: number | string) =>
      `${BASE}/teams/${id}/members/${personId}`,
    removeMember: (id: number | string, personId: number | string) =>
      `${BASE}/teams/${id}/members/${personId}`,
  },
  dashboard: {
    overview: () => `${BASE}/dashboard/overview`,
    statusMix: () => `${BASE}/dashboard/status-mix`,
    progressOverTime: () => `${BASE}/dashboard/progress-over-time`,
    teamLeaderboard: () => `${BASE}/dashboard/team-leaderboard`,
    peopleSummary: () => `${BASE}/dashboard/people-summary`,
    search: () => `${BASE}/dashboard/search`,
  },
} as const
