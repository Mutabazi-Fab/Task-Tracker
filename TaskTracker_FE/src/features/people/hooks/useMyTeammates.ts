import { useQueries } from '@tanstack/react-query'
import { fetchTeamMembers } from '../../teams/api/teams.api'
import type { TeamMember } from '../../../types/team.types'

/**
 * A Member's People page is scoped to the roster of every team they belong to (usually
 * one), not the whole org — reuses the same GET /teams/{id}/members endpoint and query
 * key as useTeamMembers, so results are shared/deduped with anything else on the team
 * page that's already fetched them.
 */
export function useMyTeammates(teamIds: number[]) {
  const results = useQueries({
    queries: teamIds.map((teamId) => ({
      queryKey: ['people', 'by-team', teamId],
      queryFn: () => fetchTeamMembers(teamId),
      enabled: Number.isFinite(teamId),
    })),
  })

  const isLoading = results.some((r) => r.isLoading)
  const isError = results.some((r) => r.isError)
  const error = results.find((r) => r.error)?.error as Error | undefined

  const teammates: TeamMember[] = []
  const seen = new Set<number>()
  for (const result of results) {
    for (const member of result.data ?? []) {
      if (!seen.has(member.personId)) {
        seen.add(member.personId)
        teammates.push(member)
      }
    }
  }

  return { teammates, isLoading, isError, error }
}
