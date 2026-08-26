import { Link } from 'react-router-dom'
import { ROUTES } from '../../../app/routes'
import { Avatar } from '../../../components/ui/Avatar'
import type { Person } from '../../../types/person.types'
import styles from './TeamMemberChip.module.css'

/** One member, links to their profile. */
export function TeamMemberChip({ person }: { person: Person }) {
  return (
    <Link to={ROUTES.personProfile(person.id)} className={styles.chip}>
      <Avatar name={person.fullName} size="sm" />
      <span className={styles.name}>{person.fullName}</span>
    </Link>
  )
}
