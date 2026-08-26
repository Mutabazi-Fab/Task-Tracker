import type { ReactNode } from 'react'
import type { UseQueryResult } from '@tanstack/react-query'
import type { ApiError } from '../../api/axiosClient'
import { LoadingSpinner } from '../ui/LoadingSpinner'
import { ErrorMessage } from '../ui/ErrorMessage'

interface QueryBoundaryProps<TData> {
  query: UseQueryResult<TData, ApiError>
  children: (data: TData) => ReactNode
}

/**
 * One wrapper handling isLoading/isError for any query, so no page or
 * component hand-rolls its own loading/error branches.
 */
export function QueryBoundary<TData>({ query, children }: QueryBoundaryProps<TData>) {
  if (query.isLoading) return <LoadingSpinner />
  if (query.isError) return <ErrorMessage message={query.error.message} />
  return <>{children(query.data as TData)}</>
}
