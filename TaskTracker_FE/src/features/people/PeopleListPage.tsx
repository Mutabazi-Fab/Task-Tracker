import { Link } from 'react-router-dom'
import { ROUTES } from '../../app/routes'
import { PageHeader } from '../../components/layout/PageHeader'
import { Card } from '../../components/ui/Card'
import { Avatar } from '../../components/ui/Avatar'
import { QueryBoundary } from '../../components/feedback/QueryBoundary'
import { EmptyState } from '../../components/ui/EmptyState'
import { usePeople } from './hooks/usePeople'
import styles from './PeopleListPage.module.css'

/** No dedicated row component here by design — the tree only breaks out
 * PersonProfilePage's own sections; the list itself stays this simple. */
export function PeopleListPage() {
  const query = usePeople()

  return (
    <>
      <PageHeader breadcrumb="Throughline" title="People" />
      <QueryBoundary query={query}>
        {(people) =>
          people.length === 0 ? (
            <EmptyState title="No people yet" />
          ) : (
            <div className={styles.grid}>
              {people.map((person) => (
                <Link key={person.id} to={ROUTES.personProfile(person.id)} className={styles.link}>
                  <Card padding="sm">
                    <div className={styles.row}>
                      <Avatar name={person.fullName} />
                      <div className={styles.identity}>
                        <span className={styles.name}>{person.fullName}</span>
                        <span className={styles.role}>{person.role}</span>
                        <span className={styles.team}>{person.teamName ?? 'Unassigned'}</span>
                      </div>
                    </div>
                  </Card>
                </Link>
              ))}
            </div>
          )
        }
      </QueryBoundary>
    </>
  )
}
