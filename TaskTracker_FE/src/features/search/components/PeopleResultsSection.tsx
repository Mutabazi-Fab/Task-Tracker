import { Link } from 'react-router-dom'
import { ROUTES } from '../../../app/routes'
import { Card } from '../../../components/ui/Card'
import { Avatar } from '../../../components/ui/Avatar'
import { EmptyState } from '../../../components/ui/EmptyState'
import type { Person } from '../../../types/person.types'
import styles from './PeopleResultsSection.module.css'

export function PeopleResultsSection({ people }: { people: Person[] }) {
  if (people.length === 0) {
    return <EmptyState title="No matching people" />
  }

  return (
    <div className={styles.grid}>
      {people.map((person) => (
        <Link key={person.id} to={ROUTES.personProfile(person.id)} className={styles.link}>
          <Card padding="sm">
            <div className={styles.row}>
              <Avatar name={person.fullName} />
              <div className={styles.identity}>
                <span className={styles.name}>{person.fullName}</span>
                <span className={styles.role}>{person.role}</span>
              </div>
            </div>
          </Card>
        </Link>
      ))}
    </div>
  )
}
