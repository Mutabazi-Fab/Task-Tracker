import { Link } from 'react-router-dom'
import { ROUTES } from '../../../app/routes'
import { Avatar } from '../../../components/ui/Avatar'
import { Card } from '../../../components/ui/Card'
import { EmptyState } from '../../../components/ui/EmptyState'
import { ErrorMessage } from '../../../components/ui/ErrorMessage'
import { LoadingSpinner } from '../../../components/ui/LoadingSpinner'
import { useAuth } from '../../auth/useAuth'
import { useMyTeammates } from '../hooks/useMyTeammates'
import styles from '../PeopleListPage.module.css'

/**
 * A Member's People page — only the roster of the team(s) they belong to, not the org.
 * No team at all means nothing to show here, so that's a plain, honest empty state
 * rather than an empty grid that looks broken.
 */
export function MyTeammatesGrid() {
  const { currentUser } = useAuth()
  const teamIds = currentUser?.teams.map((t) => t.teamId) ?? []
  const { teammates, isLoading, isError, error } = useMyTeammates(teamIds)

  if (teamIds.length === 0) {
    return (
      <EmptyState
        title="You're not part of a team yet"
        description="Once a Director adds you to a team, your teammates will show up here."
      />
    )
  }

  if (isLoading) return <LoadingSpinner label="Loading your team" />
  if (isError) return <ErrorMessage message={error?.message ?? 'Something went wrong.'} />
  if (teammates.length === 0) return <EmptyState title="No teammates yet" />

  return (
    <div className={styles.grid}>
      {teammates.map((member) => (
        <Link key={member.personId} to={ROUTES.personProfile(member.personId)} className={styles.link}>
          <Card padding="sm">
            <div className={styles.row}>
              <Avatar name={member.fullName} />
              <div className={styles.identity}>
                <span className={styles.name}>{member.fullName}</span>
                <span className={styles.role}>
                  {member.jobTitle}
                  {member.isLeader ? ' · Team Leader' : ''}
                </span>
              </div>
            </div>
          </Card>
        </Link>
      ))}
    </div>
  )
}
