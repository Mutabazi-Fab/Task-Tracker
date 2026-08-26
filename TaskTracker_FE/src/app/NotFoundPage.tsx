import { EmptyState } from '../components/ui/EmptyState'
import { PageHeader } from '../components/layout/PageHeader'

/** Catch-all for routes whose feature hasn't been built yet, or a genuine bad URL. */
export function NotFoundPage() {
  return (
    <>
      <PageHeader title="Not found" />
      <EmptyState title="Nothing here" description="This section either doesn't exist or hasn't been built yet." />
    </>
  )
}
