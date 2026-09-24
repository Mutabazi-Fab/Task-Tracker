import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ROUTES } from '../../app/routes'
import { PageHeader } from '../../components/layout/PageHeader'
import { Card } from '../../components/ui/Card'
import { Button } from '../../components/ui/Button'
import { Icon } from '../../components/ui/Icon'
import { Pagination } from '../../components/ui/Pagination'
import { SelectField } from '../../components/ui/SelectField'
import { TextField } from '../../components/ui/TextField'
import { QueryBoundary } from '../../components/feedback/QueryBoundary'
import { useAuth } from '../auth/useAuth'
import { useMarkCategoryRead } from '../notifications/hooks/useMarkCategoryRead'
import { IncidentKpiRow } from './components/IncidentKpiRow'
import { IncidentBreakdownChart } from './components/IncidentBreakdownChart'
import { IncidentTrendChart } from './components/IncidentTrendChart'
import { IncidentTable } from './components/IncidentTable'
import { CreateIncidentModal } from './components/CreateIncidentModal'
import { useIncidents } from './hooks/useIncidents'
import { useIncidentDashboard } from './hooks/useIncidentDashboard'
import { categoryLabel, statusLabel } from './lib/incidentLabels'
import { useBusinessUnitOptions } from './hooks/useBusinessUnitOptions'
import { severityChartColor, statusChartColor } from './lib/chartColors'
import type { IncidentBusinessUnit, IncidentCategory, IncidentSeverity, IncidentStatus } from '../../types/incident.types'
import styles from './IncidentDashboardPage.module.css'

const PAGE_SIZE = 10

const STATUS_OPTIONS: IncidentStatus[] = ['OPEN', 'UNDER_INVESTIGATION', 'MONITORING', 'CLOSED', 'REJECTED_NOT_AN_INCIDENT']
const SEVERITY_OPTIONS: IncidentSeverity[] = ['LOW', 'MODERATE', 'HIGH', 'CRITICAL']
const CATEGORY_OPTIONS: IncidentCategory[] = [
  'INTERNAL_FRAUD', 'EXTERNAL_FRAUD', 'EMPLOYMENT_PRACTICES_SAFETY', 'CLIENTS_PRODUCTS_BUSINESS_PRACTICES',
  'PHYSICAL_ASSET_DAMAGE', 'ICT_SYSTEMS', 'CYBERSECURITY', 'PROCESS_EXECUTION', 'BUSINESS_DISRUPTION_BCM',
  'COMPLIANCE_LEGAL', 'THIRD_PARTY_OUTSOURCING',
]

/** The Incident Management module's home — dashboard KPIs/breakdowns/trend up top, a filterable,
 *  searchable, paginated incident list below, and a "Report incident" action (Director/Executive/Super
 *  Admin only — Role.isAtLeastDirector on the backend). */
export function IncidentDashboardPage() {
  const { isDirector } = useAuth()
  const navigate = useNavigate()
  const [status, setStatus] = useState<IncidentStatus | ''>('')
  const [severity, setSeverity] = useState<IncidentSeverity | ''>('')
  const [category, setCategory] = useState<IncidentCategory | ''>('')
  const [businessUnit, setBusinessUnit] = useState<IncidentBusinessUnit | ''>('')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [createOpen, setCreateOpen] = useState(false)
  const markCategoryRead = useMarkCategoryRead()

  // Clears the Incidents badge for incidents shared with this person, once they open the page.
  useEffect(() => {
    markCategoryRead.mutate(['INCIDENT_ACCESS_GRANTED'])
  }, [])

  const businessUnits = useBusinessUnitOptions()
  const dashboardQuery = useIncidentDashboard()
  const listQuery = useIncidents({
    status: status || undefined,
    severity: severity || undefined,
    category: category || undefined,
    businessUnit: businessUnit || undefined,
    q: search || undefined,
    page,
    size: PAGE_SIZE,
    sort: 'createdAt,desc',
  })

  function resetPage<T>(setter: (v: T) => void) {
    return (v: T) => {
      setter(v)
      setPage(0)
    }
  }

  /** Clears every filter and search term at once — a one-click way back to "every incident,
   *  no filter applied" from anywhere the list has been narrowed down to. */
  function handleShowAll() {
    setStatus('')
    setSeverity('')
    setCategory('')
    setBusinessUnit('')
    setSearch('')
    setPage(0)
  }

  return (
    <>
      <PageHeader
        breadcrumb="Throughline"
        title="Incident Management"
        right={
          <div className={styles.headerActions}>
            <Button variant="primary" className={styles.guidanceButton} onClick={() => navigate(ROUTES.incidentGuidance)}>
              <Icon name="shield" size={16} />
              Guidance
            </Button>
            {isDirector && <Button onClick={() => setCreateOpen(true)}>Report incident</Button>}
          </div>
        }
      />

      <div className={styles.kpiStack}>
        <IncidentKpiRow />
      </div>

      <div className={styles.charts}>
        <Card>
          <div className={styles.sectionHeadingLg}>By status</div>
          <QueryBoundary query={dashboardQuery}>
            {(d) => (
              <IncidentBreakdownChart data={d.statusBreakdown} labelFor={(l) => statusLabel(l as IncidentStatus)} colorFor={statusChartColor} />
            )}
          </QueryBoundary>
        </Card>
        <Card>
          <div className={styles.sectionHeadingLg}>By severity</div>
          <QueryBoundary query={dashboardQuery}>
            {(d) => (
              <IncidentBreakdownChart
                data={d.severityBreakdown}
                labelFor={(l) => l.charAt(0) + l.slice(1).toLowerCase()}
                colorFor={severityChartColor}
              />
            )}
          </QueryBoundary>
        </Card>
        <Card>
          <div className={styles.sectionHeadingLg}>By category</div>
          <QueryBoundary query={dashboardQuery}>
            {(d) => <IncidentBreakdownChart data={d.categoryBreakdown} labelFor={(l) => categoryLabel(l as IncidentCategory)} />}
          </QueryBoundary>
        </Card>
      </div>

      <Card>
        <div className={styles.sectionHeadingLg}>Incidents per month</div>
        <QueryBoundary query={dashboardQuery}>{(d) => <IncidentTrendChart data={d.monthlyTrend} />}</QueryBoundary>
      </Card>

      <QueryBoundary query={dashboardQuery}>
        {(d) => (
          <Card>
            <div className={styles.sectionHeadingLg}>SLA compliance</div>
            <p>
              {d.closedCount} incident{d.closedCount === 1 ? '' : 's'} closed so far — {d.closedLateCount} closed after
              their target date. On-time close rate: {d.slaComplianceRate.toFixed(0)}%.
            </p>
          </Card>
        )}
      </QueryBoundary>

      <div className={styles.listHeadingRow}>
        <div className={styles.sectionHeadingLg}>All incidents</div>
        <Button variant="secondary" onClick={handleShowAll}>
          Check all incidents
        </Button>
      </div>

      <div className={styles.controls}>
        <div className={styles.filters}>
          <div className={styles.filterField}>
            <SelectField
              value={status}
              onChange={(v) => resetPage(setStatus)(v as IncidentStatus | '')}
              placeholder="All statuses"
              placeholderSelectable
              options={STATUS_OPTIONS.map((s) => ({ label: statusLabel(s), value: s }))}
            />
          </div>
          <div className={styles.filterField}>
            <SelectField
              value={severity}
              onChange={(v) => resetPage(setSeverity)(v as IncidentSeverity | '')}
              placeholder="All severities"
              placeholderSelectable
              options={SEVERITY_OPTIONS.map((s) => ({ label: s.charAt(0) + s.slice(1).toLowerCase(), value: s }))}
            />
          </div>
          <div className={styles.filterField}>
            <SelectField
              value={category}
              onChange={(v) => resetPage(setCategory)(v as IncidentCategory | '')}
              placeholder="All categories"
              placeholderSelectable
              options={CATEGORY_OPTIONS.map((c) => ({ label: categoryLabel(c), value: c }))}
            />
          </div>
          <div className={styles.filterField}>
            <SelectField
              value={businessUnit}
              onChange={(v) => resetPage(setBusinessUnit)(v as IncidentBusinessUnit | '')}
              placeholder="All business units"
              placeholderSelectable
              options={businessUnits.options}
            />
          </div>
        </div>
        <TextField
          value={search}
          onChange={resetPage(setSearch)}
          placeholder="Search title or code…"
          aria-label="Search incidents"
        />
      </div>

      <QueryBoundary query={listQuery}>{(result) => <IncidentTable incidents={result.content} />}</QueryBoundary>
      {listQuery.data && (
        <div className={styles.pagination}>
          <Pagination page={page} totalPages={listQuery.data.totalPages} onChange={setPage} />
        </div>
      )}

      {isDirector && <CreateIncidentModal open={createOpen} onClose={() => setCreateOpen(false)} />}
    </>
  )
}
