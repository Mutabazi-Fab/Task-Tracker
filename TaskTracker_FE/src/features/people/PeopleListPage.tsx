import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ROUTES } from '../../app/routes'
import { PageHeader } from '../../components/layout/PageHeader'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { Avatar } from '../../components/ui/Avatar'
import { QueryBoundary } from '../../components/feedback/QueryBoundary'
import { EmptyState } from '../../components/ui/EmptyState'
import { useAuth } from '../auth/useAuth'
import { usePeople } from './hooks/usePeople'
import { CreatePersonModal } from './components/CreatePersonModal'
import styles from './PeopleListPage.module.css'

const ROLE_LABEL: Record<string, string> = {
  DIRECTOR: 'Director',
  SUPER_ADMIN: 'Super Admin',
}

/** No dedicated row component here by design — the tree only breaks out
 * PersonProfilePage's own sections; the list itself stays this simple. */
export function PeopleListPage() {
  const query = usePeople()
  const { isDirector } = useAuth()
  const [createOpen, setCreateOpen] = useState(false)

  return (
    <>
      <PageHeader
        breadcrumb="Throughline"
        title="People"
        right={isDirector ? <Button onClick={() => setCreateOpen(true)}>New person</Button> : undefined}
      />
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
                        <span className={styles.role}>
                          {person.jobTitle}
                          {person.role && ROLE_LABEL[person.role] ? ` · ${ROLE_LABEL[person.role]}` : ''}
                        </span>
                        <span className={styles.team}>
                          {person.teams.length > 0
                            ? person.teams.map((t) => t.teamName).join(', ')
                            : 'Unassigned'}
                        </span>
                      </div>
                    </div>
                  </Card>
                </Link>
              ))}
            </div>
          )
        }
      </QueryBoundary>

      {isDirector && <CreatePersonModal open={createOpen} onClose={() => setCreateOpen(false)} />}
    </>
  )
}
