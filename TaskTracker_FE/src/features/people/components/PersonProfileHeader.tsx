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
        <p className={styles.role}>{person.role}</p>
        <p className={styles.unit}>{person.teamName ? `Unit: ${person.teamName}` : 'Unassigned'}</p>
      </div>
    </div>
  )
}
