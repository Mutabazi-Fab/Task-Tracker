import { AccountStatusBadge } from '../../../components/ui/AccountStatusBadge'
import { Avatar } from '../../../components/ui/Avatar'
import type { Person } from '../../../types/person.types'
import styles from './PersonProfileHeader.module.css'

/** Always resolves to something — a null role (a legacy account) is treated as Member
 *  everywhere else in the app, so it reads the same way here. */
const ROLE_LABEL: Record<string, string> = {
  DIRECTOR: 'Director',
  SUPER_ADMIN: 'Super Admin',
  MEMBER: 'Member',
}

/** Avatar, name (+ account status right beside it), role, unit. */
export function PersonProfileHeader({ person }: { person: Person }) {
  const roleLabel = ROLE_LABEL[person.role ?? 'MEMBER']
  const isTeamLeader = person.teams.some((t) => t.isLeader)

  return (
    <div className={styles.wrap}>
      <Avatar name={person.fullName} size="lg" />
      <div className={styles.identity}>
        <div className={styles.nameRow}>
          <h2 className={styles.name}>{person.fullName}</h2>
          <AccountStatusBadge active={person.active} />
        </div>
        <p className={styles.role}>
          {person.jobTitle} · {roleLabel}
          {isTeamLeader ? ' · Team Leader' : ''}
        </p>
        <p className={styles.unit}>
          {person.teams.length > 0 ? `Unit: ${person.teams.map((t) => t.teamName).join(', ')}` : 'Unassigned'}
        </p>
      </div>
    </div>
  )
}
