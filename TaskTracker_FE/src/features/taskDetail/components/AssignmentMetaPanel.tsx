import { Card } from '../../../components/ui/Card'
import { StatusChip } from '../../../components/ui/StatusChip'
import { formatDate } from '../../../lib/formatDate'
import { useTeam } from '../../teams/hooks/useTeam'
import { useTeamMembers } from '../../teams/hooks/useTeamMembers'
import { TeamMemberChip } from '../../teams/components/TeamMemberChip'
import type { TaskDetail } from '../../../types/task.types'
import styles from './AssignmentMetaPanel.module.css'

/** Assigned to / by / date / status / reassign count — the at-a-glance ownership facts.
 *  When the task is team-assigned, task.assigneeId is that team's id, so its leader is one
 *  more fetch away — worth showing here since "who's actually accountable for this" is
 *  exactly what this panel exists to answer. The full roster below it answers the natural
 *  follow-up — "who's actually on that team" — without a click away to the Teams page;
 *  read-only here (no onMakeLeader/onRemove), same chip Teams itself uses so the leader
 *  reads the same green "Leader" tag everywhere in the app. */
export function AssignmentMetaPanel({ task }: { task: TaskDetail }) {
  const isTeamAssigned = task.assigneeType === 'TEAM'
  const teamQuery = useTeam(isTeamAssigned ? task.assigneeId ?? NaN : NaN)
  const membersQuery = useTeamMembers(isTeamAssigned ? task.assigneeId ?? NaN : NaN)

  return (
    <Card>
      <div className={styles.grid}>
        <div className={styles.item}>
          <span className={styles.label}>Assigned to</span>
          <span className={styles.value}>
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
        <div className={styles.item}>
          <span className={styles.label}>Assigned by</span>
          <span className={styles.value}>{task.assignedByName}</span>
        </div>
        <div className={styles.item}>
          <span className={styles.label}>Date assigned</span>
          <span className={styles.value}>{formatDate(task.dateAssigned)}</span>
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

      {isTeamAssigned && (
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
