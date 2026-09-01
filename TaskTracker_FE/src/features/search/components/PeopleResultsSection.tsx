import { Link } from 'react-router-dom'
import { ROUTES } from '../../../app/routes'
import { Card } from '../../../components/ui/Card'
import { Avatar } from '../../../components/ui/Avatar'
import { EmptyState } from '../../../components/ui/EmptyState'
import { formatPercentage } from '../../../lib/formatPercentage'
import type { PersonSearchResult } from '../../../types/dashboard.types'
import styles from './PeopleResultsSection.module.css'

/**
 * Each match shows its per-team stats breakdown — e.g. "Digital Banking 60% · Payments
 * 20%" — not one blended number across every team the person belongs to. That's the
 * whole reason this is PersonSearchResult (person + teamBreakdown) rather than a bare
 * Person: a search result is exactly the place the spec called out for this.
 */
export function PeopleResultsSection({ people }: { people: PersonSearchResult[] }) {
  if (people.length === 0) {
    return <EmptyState title="No matching people" />
  }

  return (
    <div className={styles.grid}>
      {people.map(({ person, teamBreakdown }) => (
        <Link key={person.id} to={ROUTES.personProfile(person.id)} className={styles.link}>
          <Card padding="sm">
            <div className={styles.row}>
              <Avatar name={person.fullName} />
              <div className={styles.identity}>
                <span className={styles.name}>{person.fullName}</span>
                <span className={styles.role}>{person.jobTitle}</span>
              </div>
            </div>
            {teamBreakdown.length > 0 && (
              <div className={styles.breakdown}>
                {teamBreakdown.map((t) => (
                  <span key={t.teamId} className={styles.breakdownItem}>
                    {t.teamName} {formatPercentage(t.averageProgress)}
                  </span>
                ))}
              </div>
            )}
          </Card>
        </Link>
      ))}
    </div>
  )
}
