import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ROUTES } from '../../app/routes'
import { PageHeader } from '../../components/layout/PageHeader'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { QueryBoundary } from '../../components/feedback/QueryBoundary'
import { EmptyState } from '../../components/ui/EmptyState'
import { useAuth } from '../auth/useAuth'
import { useMarkCategoryRead } from '../notifications/hooks/useMarkCategoryRead'
import { useDepartments } from './hooks/useDepartments'
import { CreateDepartmentModal } from './components/CreateDepartmentModal'
import styles from './DepartmentsListPage.module.css'

/** Every authenticated person can see the department list (they need it to make sense of
 *  where a team or a colleague sits in the org chart) — creating one is Executive-or-above
 *  (the CEO stands up a new department herself, same as Super Admin can); renaming one or
 *  reassigning its head stays Super-Admin-only (see DepartmentAdminControls on DepartmentPage). */
export function DepartmentsListPage() {
  const query = useDepartments()
  const { isDirector, isExecutive } = useAuth()
  const [createOpen, setCreateOpen] = useState(false)
  const markCategoryRead = useMarkCategoryRead()

  // Clears the Sidebar's "new department" badge — only Director-or-above ever sees that
  // badge in the first place (see Sidebar's getNavItems), so this is a no-op for anyone
  // else who happens to land on this otherwise-open-to-everyone page.
  useEffect(() => {
    if (isDirector) markCategoryRead.mutate(['DEPARTMENT_CREATED'])
  }, [isDirector])

  return (
    <>
      <PageHeader
        breadcrumb="Throughline"
        title="Departments"
        right={isExecutive ? <Button onClick={() => setCreateOpen(true)}>New department</Button> : undefined}
      />
      <QueryBoundary query={query}>
        {(departments) =>
          departments.length === 0 ? (
            <EmptyState title="No departments yet" />
          ) : (
            <div className={styles.grid}>
              {departments.map((department) => (
                <Link key={department.id} to={ROUTES.department(department.id)} className={styles.link}>
                  <Card padding="sm">
                    <span className={styles.name}>{department.name}</span>
                    <span className={styles.head}>
                      {department.headDirectorName ? `Headed by ${department.headDirectorName}` : 'No head assigned'}
                    </span>
                    <span className={styles.meta}>{department.teamCount} teams</span>
                  </Card>
                </Link>
              ))}
            </div>
          )
        }
      </QueryBoundary>

      {isExecutive && <CreateDepartmentModal open={createOpen} onClose={() => setCreateOpen(false)} />}
    </>
  )
}
