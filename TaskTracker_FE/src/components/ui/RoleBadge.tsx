import type { Role } from '../../types/person.types'
import styles from './RoleBadge.module.css'

/** The five states worth calling out at a glance: a Super Admin, an Executive (the CEO's seat), a
 *  Director, a Member who leads at least one of their teams, or a plain Member. */
export type BadgeRole = 'SUPER_ADMIN' | 'EXECUTIVE' | 'DIRECTOR' | 'TEAM_LEADER' | 'MEMBER'

const LABEL: Record<BadgeRole, string> = {
  SUPER_ADMIN: 'Super Admin',
  EXECUTIVE: 'Executive',
  DIRECTOR: 'Director',
  TEAM_LEADER: 'Team Leader',
  MEMBER: 'Member',
}

const STYLE: Record<BadgeRole, string> = {
  SUPER_ADMIN: styles.superAdmin,
  EXECUTIVE: styles.executive,
  DIRECTOR: styles.director,
  TEAM_LEADER: styles.teamLeader,
  MEMBER: styles.member,
}

/** Works out which of the five badge states applies from a raw role + team memberships,
 *  so every caller (Sidebar, profile pages, ...) reaches the same answer the same way. */
export function resolveBadgeRole(role: Role | null, isTeamLeader: boolean): BadgeRole {
  if (role === 'SUPER_ADMIN') return 'SUPER_ADMIN'
  if (role === 'EXECUTIVE') return 'EXECUTIVE'
  if (role === 'DIRECTOR') return 'DIRECTOR'
  return isTeamLeader ? 'TEAM_LEADER' : 'MEMBER'
}

export function RoleBadge({ badgeRole }: { badgeRole: BadgeRole }) {
  return <span className={`${styles.badge} ${STYLE[badgeRole]}`}>{LABEL[badgeRole]}</span>
}
