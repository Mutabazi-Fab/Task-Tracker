import { useQueries } from '@tanstack/react-query'
import { fetchTeamMembers } from '../../teams/api/teams.api'
import { useAuth } from '../../auth/useAuth'
import type { TaskDetail } from '../../../types/task.types'

/** Whether the viewer sees the "Log progress" form on this task. Mirrors the backend rule in
 *  TaskServiceImpl.addProgressComment (which is what actually enforces it): only for an
 *  individually-tracked task, and only for the person it's assigned to, the leader of a team
 *  that person belongs to (just their own team(s)), or a Director/Executive/Super Admin.
 *  Team membership is only fetched for a viewer who leads a team and isn't already allowed
 *  another way, so an ordinary viewer costs no extra requests. */
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
