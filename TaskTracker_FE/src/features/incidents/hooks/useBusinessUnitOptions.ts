import { useMemo } from 'react'
import { useDepartments } from '../../departments/hooks/useDepartments'

export interface BusinessUnitOption {
  label: string
  value: string
}

/** The Business Unit / Branch choices for an incident: every Department that currently
 *  exists, plus the catch-all "Other" carried over from the Excel's list. Driven by the live
 *  department list (same query the Departments page uses), so a department a CEO or Super
 *  Admin creates appears here immediately — nothing to keep in sync by hand. The backend
 *  independently rejects a value that isn't one of these (IncidentServiceImpl.resolveBusinessUnit). */
export function useBusinessUnitOptions(): { options: BusinessUnitOption[]; isLoading: boolean } {
  const departmentsQuery = useDepartments()

  const options = useMemo(() => {
    const names = (departmentsQuery.data ?? []).map((d) => d.name).sort((a, b) => a.localeCompare(b))
    return [...names, 'Other'].map((name) => ({ label: name, value: name }))
  }, [departmentsQuery.data])

  return { options, isLoading: departmentsQuery.isLoading }
}
