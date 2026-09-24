import { useState } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import { ROUTES } from '../../app/routes'
import { PageHeader } from '../../components/layout/PageHeader'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { QueryBoundary } from '../../components/feedback/QueryBoundary'
import { EmptyState } from '../../components/ui/EmptyState'
import { useAuth } from '../auth/useAuth'
import { useDepartment } from './hooks/useDepartment'
import { useTeams } from '../teams/hooks/useTeams'
import { CreateTeamModal } from '../teams/components/CreateTeamModal'
import { DeleteDepartmentModal } from './components/DeleteDepartmentModal'
import { DepartmentAdminControls } from './components/DepartmentAdminControls'
import styles from './DepartmentPage.module.css'

/** Name, head Director, and every Team that belongs to it — open to any authenticated caller (read
 *  access is open org-chart-wide). */
export function DepartmentPage() {
  const { departmentId } = useParams<{ departmentId: string }>()
  const id = Number(departmentId)

  const departmentQuery = useDepartment(id)
  const teamsQuery = useTeams()
  const { currentUser, isDirector, isExecutive } = useAuth()
  const navigate = useNavigate()
  const [createTeamOpen, setCreateTeamOpen] = useState(false)
  const [deleteOpen, setDeleteOpen] = useState(false)

  return (
    <QueryBoundary query={departmentQuery}>
      {(department) => {
        const teams = (teamsQuery.data ?? []).filter((team) => team.departmentId === department.id)
        // Mirrors TeamServiceImpl.isHeadOfDepartment exactly: Executive/Super Admin may
        // stand up a team in any department; a plain Director only in the one they
        // actually head — never merely one they belong to.
        const canCreateTeam = isExecutive || (isDirector && currentUser?.id === department.headDirectorId)
        // Same tier as creating a department in the first place — DepartmentServiceImpl.
        // deleteDepartment refuses a plain Director even if they head it.
        const canDeleteDepartment = isExecutive

        return (
          <>
            <PageHeader
              breadcrumb="Throughline / Departments"
              title={department.name}
              onBack={() => navigate(-1)}
              right={
                canDeleteDepartment ? (
                  <Button variant="danger" onClick={() => setDeleteOpen(true)}>
                    Delete department
                  </Button>
                ) : undefined
              }
            />

            <Card>
              <div className={styles.header}>
                <span className={styles.head}>
                  {department.headDirectorName ? `Headed by ${department.headDirectorName}` : 'No head assigned'}
                </span>
                <span className={styles.meta}>{department.teamCount} teams</span>
              </div>
            </Card>

            <Card>
              <div className={styles.header}>
                <span className={styles.sectionHeading}>Teams</span>
                {canCreateTeam && (
                  <Button variant="primary" onClick={() => setCreateTeamOpen(true)}>
                    Create team
                  </Button>
                )}
              </div>
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

            {/* Super-Admin-only controls (rename, reassign head) — deleting now lives in the
                page header above, next to the department's name. */}
            {isExecutive && <DepartmentAdminControls department={department} />}

            {canCreateTeam && (
              <CreateTeamModal
                open={createTeamOpen}
                onClose={() => setCreateTeamOpen(false)}
                fixedDepartmentId={department.id}
                fixedDepartmentName={department.name}
              />
            )}

            {canDeleteDepartment && (
              <DeleteDepartmentModal
                departmentId={department.id}
                departmentName={department.name}
                open={deleteOpen}
                onClose={() => setDeleteOpen(false)}
                onDeleted={() => navigate(ROUTES.departments, { replace: true })}
              />
            )}
          </>
        )
      }}
    </QueryBoundary>
  )
}
