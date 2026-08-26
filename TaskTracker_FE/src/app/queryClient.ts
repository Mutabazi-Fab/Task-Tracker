import { QueryClient } from '@tanstack/react-query'
import type { ApiError } from '../api/axiosClient'

// Registers ApiError as the default TError for every useQuery/useMutation in
// the app, so QueryBoundary (and every hook) sees the axios interceptor's
// normalized shape instead of the generic `Error` TanStack Query defaults to.
declare module '@tanstack/react-query' {
  interface Register {
    defaultError: ApiError
  }
}

/**
 * One shared React Query client for the whole app. Server state (tasks,
 * people, teams, dashboard numbers) always goes through this — never
 * useState/useEffect + axios directly in a component.
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: 1,
      refetchOnWindowFocus: false,
    },
    mutations: {
      retry: 0,
    },
  },
})
