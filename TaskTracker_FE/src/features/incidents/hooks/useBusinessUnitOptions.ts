import { useMemo } from 'react'
import { useDepartments } from '../../departments/hooks/useDepartments'

export interface BusinessUnitOption {
  label: string
  value: string
}

/** The Business Unit / Branch choices for an incident: every Department that currently exists, plus the
 *  catch-all "Other" carried over from the Excel's list. */
export function useBusinessUnitOptions(): { options: BusinessUnitOption[]; isLoading: boolean } {
  const departmentsQuery = useDepartments()

  const options = useMemo(() => {
    const names = (departmentsQuery.data ?? []).map((d) => d.name).sort((a, b) => a.localeCompare(b))
    return [...names, 'Other'].map((name) => ({ label: name, value: name }))
  }, [departmentsQuery.data])

  return { options, isLoading: departmentsQuery.isLoading }
}
