import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ROUTES } from '../../app/routes'
import { PageHeader } from '../../components/layout/PageHeader'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { QueryBoundary } from '../../components/feedback/QueryBoundary'
import { EmptyState } from '../../components/ui/EmptyState'
import { useAuth } from '../auth/useAuth'
import { useDepartments } from './hooks/useDepartments'
import { CreateDepartmentModal } from './components/CreateDepartmentModal'
import styles from './DepartmentsListPage.module.css'

/** Every authenticated person can see the department list (they need it to make sense of
 *  where a team or a colleague sits in the org chart) — only a Super Admin can create one,
 *  same "open read, gated write" split as everywhere else governance-shaped in this app. */
export function DepartmentsListPage() {
  const query = useDepartments()
  const { isSuperAdmin } = useAuth()
  const [createOpen, setCreateOpen] = useState(false)

  return (
    <>
      <PageHeader
        breadcrumb="Throughline"
        title="Departments"
        right={isSuperAdmin ? <Button onClick={() => setCreateOpen(true)}>New department</Button> : undefined}
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

      {isSuperAdmin && <CreateDepartmentModal open={createOpen} onClose={() => setCreateOpen(false)} />}
    </>
  )
}
