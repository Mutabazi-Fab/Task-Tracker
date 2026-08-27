import { Avatar } from '../../../components/ui/Avatar'
import type { Person } from '../../../types/person.types'
import styles from './PersonProfileHeader.module.css'

/** Avatar, name, role, unit. */
export function PersonProfileHeader({ person }: { person: Person }) {
  return (
    <div className={styles.wrap}>
      <Avatar name={person.fullName} size="lg" />
      <div className={styles.identity}>
        <h2 className={styles.name}>{person.fullName}</h2>
        <p className={styles.role}>
          {person.jobTitle}
          {person.role === 'DIRECTOR' ? ' · Director' : ''}
        </p>
        <p className={styles.unit}>
          {person.teams.length > 0 ? `Unit: ${person.teams.map((t) => t.teamName).join(', ')}` : 'Unassigned'}
        </p>
      </div>
    </div>
  )
}
