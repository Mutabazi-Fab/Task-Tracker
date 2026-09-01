import type { Role } from '../../types/person.types'
import styles from './RoleBadge.module.css'

/** The four states worth calling out at a glance: a Super Admin, a Director, a Member who
 *  leads at least one of their teams, or a plain Member. Team Leader isn't a Role value on
 *  the backend (leadership is scoped per-team, see PersonTeamMembership.isLeader) — it's
 *  derived here from whether any of the person's team memberships has isLeader set. */
export type BadgeRole = 'SUPER_ADMIN' | 'DIRECTOR' | 'TEAM_LEADER' | 'MEMBER'

const LABEL: Record<BadgeRole, string> = {
  SUPER_ADMIN: 'Super Admin',
  DIRECTOR: 'Director',
  TEAM_LEADER: 'Team Leader',
  MEMBER: 'Member',
}

const STYLE: Record<BadgeRole, string> = {
  SUPER_ADMIN: styles.superAdmin,
  DIRECTOR: styles.director,
  TEAM_LEADER: styles.teamLeader,
  MEMBER: styles.member,
}

/** Works out which of the four badge states applies from a raw role + team memberships,
 *  so every caller (Sidebar, profile pages, ...) reaches the same answer the same way. */
export function resolveBadgeRole(role: Role | null, isTeamLeader: boolean): BadgeRole {
  if (role === 'SUPER_ADMIN') return 'SUPER_ADMIN'
  if (role === 'DIRECTOR') return 'DIRECTOR'
  return isTeamLeader ? 'TEAM_LEADER' : 'MEMBER'
}

export function RoleBadge({ badgeRole }: { badgeRole: BadgeRole }) {
  return <span className={`${styles.badge} ${STYLE[badgeRole]}`}>{LABEL[badgeRole]}</span>
}
