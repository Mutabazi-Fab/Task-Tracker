import { Card } from '../../../components/ui/Card'
import { StatusChip } from '../../../components/ui/StatusChip'
import { formatDate } from '../../../lib/formatDate'
import { useAuth } from '../../auth/useAuth'
import { useTeam } from '../../teams/hooks/useTeam'
import { useTeamMembers } from '../../teams/hooks/useTeamMembers'
import { TeamMemberChip } from '../../teams/components/TeamMemberChip'
import { useDepartment } from '../../departments/hooks/useDepartment'
import type { TaskDetail } from '../../../types/task.types'
import type { Role } from '../../../types/person.types'
import styles from './AssignmentMetaPanel.module.css'

/** Same labels as RoleBadge/PeopleListPage's ROLE_LABEL, but plain text — a loud badge
 *  would stand out for the wrong reason among these plain value spans. */
const ASSIGNED_BY_ROLE_LABEL: Record<Role, string> = {
  SUPER_ADMIN: 'Super Admin',
  EXECUTIVE: 'Executive',
  DIRECTOR: 'Director',
  MEMBER: 'Member',
}

/** Assigned to / by / date / status / reassign count — the at-a-glance ownership facts. */
export function AssignmentMetaPanel({ task }: { task: TaskDetail }) {
  const { currentUser } = useAuth()
  const isCeo = currentUser?.role === 'EXECUTIVE'
  const isTeamAssigned = task.assigneeType === 'TEAM'
  const isDepartmentAssigned = task.assigneeType === 'DEPARTMENT'
  const teamQuery = useTeam(isTeamAssigned ? task.assigneeId ?? NaN : NaN)
  const membersQuery = useTeamMembers(isTeamAssigned ? task.assigneeId ?? NaN : NaN)
  const departmentQuery = useDepartment(isDepartmentAssigned ? task.assigneeId ?? NaN : NaN)

  return (
    <Card>
      <div className={styles.grid}>
        <div className={`${styles.item} ${styles.itemWide}`}>
          <span className={styles.label}>Assigned to</span>
          <span className={styles.value} title={`${task.assigneeName} (${task.assigneeType})`}>
            {task.assigneeName} <span className={styles.type}>({task.assigneeType})</span>
          </span>
        </div>
        {isTeamAssigned && (
          <div className={styles.item}>
            <span className={styles.label}>Team leader</span>
            <span className={styles.leaderValue}>
              {teamQuery.isLoading ? '…' : (teamQuery.data?.leaderName ?? 'No leader assigned')}
            </span>
          </div>
        )}
        {isDepartmentAssigned && (
          <div className={styles.item}>
            <span className={styles.label}>Department head</span>
            <span className={styles.leaderValue}>
              {departmentQuery.isLoading ? '…' : (departmentQuery.data?.headDirectorName ?? 'No head assigned')}
            </span>
          </div>
        )}
        <div className={`${styles.item} ${styles.itemWide}`}>
          <span className={styles.label}>Assigned by</span>
          <span
            className={styles.value}
            title={
              task.assignedByRole
                ? `${task.assignedByName} (${ASSIGNED_BY_ROLE_LABEL[task.assignedByRole]})`
                : task.assignedByName
            }
          >
            {task.assignedByName}
            {task.assignedByRole && (
              <span className={styles.type}> ({ASSIGNED_BY_ROLE_LABEL[task.assignedByRole]})</span>
            )}
          </span>
        </div>
        <div className={styles.item}>
          <span className={styles.label}>Date assigned</span>
          <span className={styles.value}>{formatDate(task.dateAssigned)}</span>
        </div>
        <div className={styles.item}>
          <span className={styles.label}>Deadline</span>
          <span className={styles.value}>{task.deadline ? formatDate(task.deadline) : 'None set'}</span>
        </div>
        <div className={styles.item}>
          <span className={styles.label}>Status</span>
          <StatusChip status={task.status} />
        </div>
        <div className={styles.item}>
          <span className={styles.label}>Reassignments</span>
          <span className={styles.value}>{task.reassignments.length}</span>
        </div>
      </div>

      {isTeamAssigned && !isCeo && (
        <div className={styles.members}>
          <span className={styles.label}>{task.assigneeName} team</span>
          <div className={styles.memberList}>
            {membersQuery.isLoading ? (
              <span className={styles.value}>…</span>
            ) : membersQuery.data && membersQuery.data.length > 0 ? (
              membersQuery.data.map((member) => <TeamMemberChip key={member.personId} member={member} />)
            ) : (
              <span className={styles.value}>No members yet</span>
            )}
          </div>
        </div>
      )}
    </Card>
  )
}
