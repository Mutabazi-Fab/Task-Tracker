import { useParams, Link, useNavigate } from 'react-router-dom'
import { ROUTES } from '../../app/routes'
import { PageHeader } from '../../components/layout/PageHeader'
import { Card } from '../../components/ui/Card'
import { QueryBoundary } from '../../components/feedback/QueryBoundary'
import { EmptyState } from '../../components/ui/EmptyState'
import { useAuth } from '../auth/useAuth'
import { useDepartment } from './hooks/useDepartment'
import { useTeams } from '../teams/hooks/useTeams'
import { DepartmentAdminControls } from './components/DepartmentAdminControls'
import styles from './DepartmentPage.module.css'

/** Name, head Director, and every Team that belongs to it — open to any authenticated
 *  caller (read access is open org-chart-wide, only writes are Super-Admin-gated). */
export function DepartmentPage() {
  const { departmentId } = useParams<{ departmentId: string }>()
  const id = Number(departmentId)

  const departmentQuery = useDepartment(id)
  const teamsQuery = useTeams()
  const { isSuperAdmin } = useAuth()
  const navigate = useNavigate()

  return (
    <QueryBoundary query={departmentQuery}>
      {(department) => {
        const teams = (teamsQuery.data ?? []).filter((team) => team.departmentId === department.id)

        return (
          <>
            <PageHeader breadcrumb="Throughline / Departments" title={department.name} onBack={() => navigate(-1)} />

            <Card>
              <div className={styles.header}>
                <span className={styles.head}>
                  {department.headDirectorName ? `Headed by ${department.headDirectorName}` : 'No head assigned'}
                </span>
                <span className={styles.meta}>{department.teamCount} teams</span>
              </div>
            </Card>

            <Card>
              <span className={styles.sectionHeading}>Teams</span>
              {teams.length === 0 ? (
                <EmptyState title="No teams in this department yet" />
              ) : (
                <div className={styles.teamList}>
                  {teams.map((team) => (
                    <Link key={team.id} to={ROUTES.team(team.id)} className={styles.teamLink}>
                      <span className={styles.teamName}>{team.name}</span>
                      <span className={styles.teamLeader}>
                        {team.leaderName ? `Led by ${team.leaderName}` : 'No leader'}
                      </span>
                    </Link>
                  ))}
                </div>
              )}
            </Card>

            {isSuperAdmin && <DepartmentAdminControls department={department} />}
          </>
        )
      }}
    </QueryBoundary>
  )
}
