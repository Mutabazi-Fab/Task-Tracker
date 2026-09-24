import { useQueries } from '@tanstack/react-query'
import { fetchTeamMembers } from '../../teams/api/teams.api'
import { useAuth } from '../../auth/useAuth'
import type { TaskDetail } from '../../../types/task.types'

/** Whether the viewer sees the "Log progress" form on this task. */
export function useCanLogProgress(task: TaskDetail): boolean {
  const { currentUser, isDirector } = useAuth()

  const isAssignee = task.assigneeId !== null && task.assigneeId === currentUser?.id
  const alreadyAllowed = isDirector || isAssignee
  const ledTeamIds =
    alreadyAllowed || task.assigneeType !== 'INDIVIDUAL' || task.assigneeId === null
      ? []
      : (currentUser?.teams ?? []).filter((t) => t.isLeader).map((t) => t.teamId)

  const memberLists = useQueries({
    queries: ledTeamIds.map((teamId) => ({
      queryKey: ['people', 'by-team', teamId],
      queryFn: () => fetchTeamMembers(teamId),
    })),
  })

  if (task.assigneeType !== 'INDIVIDUAL') return false
  if (alreadyAllowed) return true
  return memberLists.some((q) => q.data?.some((member) => member.personId === task.assigneeId))
}
