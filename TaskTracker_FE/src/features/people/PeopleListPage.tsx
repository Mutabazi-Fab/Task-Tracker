import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ROUTES } from '../../app/routes'
import { PageHeader } from '../../components/layout/PageHeader'
import { AccountStatusBadge } from '../../components/ui/AccountStatusBadge'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { Avatar } from '../../components/ui/Avatar'
import { QueryBoundary } from '../../components/feedback/QueryBoundary'
import { EmptyState } from '../../components/ui/EmptyState'
import { useAuth } from '../auth/useAuth'
import { usePeople } from './hooks/usePeople'
import { CreatePersonModal } from './components/CreatePersonModal'
import { MyTeammatesGrid } from './components/MyTeammatesGrid'
import styles from './PeopleListPage.module.css'

/** Always resolves to something — a null role (a legacy account) is treated as Member
 *  everywhere else in the app, so it reads the same way here. */
const ROLE_LABEL: Record<string, string> = {
  DIRECTOR: 'Director',
  SUPER_ADMIN: 'Super Admin',
  MEMBER: 'Member',
}

/**
 * Director/Super Admin see the whole org roster. A Member sees only their own team's
 * roster (MyTeammatesGrid) — this page doesn't even fetch the org-wide list for them
 * (see usePeople's enabled flag), not just hide it in the UI.
 */
export function PeopleListPage() {
  const { isDirector } = useAuth()
  const query = usePeople(isDirector)
  const [createOpen, setCreateOpen] = useState(false)

  if (!isDirector) {
    return (
      <>
        <PageHeader breadcrumb="Throughline" title="My Team" />
        <MyTeammatesGrid />
      </>
    )
  }

  return (
    <>
      <PageHeader
        breadcrumb="Throughline"
        title="People"
        right={<Button onClick={() => setCreateOpen(true)}>New person</Button>}
      />
      <QueryBoundary query={query}>
        {(people) =>
          people.length === 0 ? (
            <EmptyState title="No people yet" />
          ) : (
            <div className={styles.grid}>
              {people.map((person) => {
                const roleLabel = ROLE_LABEL[person.role ?? 'MEMBER']
                const isTeamLeader = person.teams.some((t) => t.isLeader)

                return (
                  <Link key={person.id} to={ROUTES.personProfile(person.id)} className={styles.link}>
                    <Card padding="sm">
                      <div className={styles.row}>
                        <Avatar name={person.fullName} />
                        <div className={styles.identity}>
                          <div className={styles.nameRow}>
                            <span className={styles.name}>{person.fullName}</span>
                            <AccountStatusBadge active={person.active} />
                          </div>
                          <span className={styles.role}>
                            {person.jobTitle} · {roleLabel}
                            {isTeamLeader ? ' · Team Leader' : ''}
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
                )
              })}
            </div>
          )
        }
      </QueryBoundary>

      <CreatePersonModal open={createOpen} onClose={() => setCreateOpen(false)} />
    </>
  )
}
