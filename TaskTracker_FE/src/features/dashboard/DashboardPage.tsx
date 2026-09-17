import { PageHeader } from '../../components/layout/PageHeader'
import { Card } from '../../components/ui/Card'
import { useAuth } from '../auth/useAuth'
import { TopLevelTasksSection } from './components/TopLevelTasksSection'
import { MyDashboardSummary } from './components/MyDashboardSummary'
import { KpiRow } from './components/KpiRow'
import { ExecutiveKpiRow } from './components/ExecutiveKpiRow'
import { ProgressOverTimeChart } from './components/ProgressOverTimeChart'
import { ProgressChartLegend } from './components/ProgressChartLegend'
import { StatusDonut } from './components/StatusDonut'
import { StatusDonutLegend } from './components/StatusDonutLegend'
import { TeamLeaderboardTable } from './components/TeamLeaderboardTable'
import { PeopleSummaryGrid } from './components/PeopleSummaryGrid'
import styles from './DashboardPage.module.css'

/**
 * Composes the sections below. A Member gets a completely different dashboard — just
 * MyDashboardSummary (their own assigned tasks and progress) — not the org-wide
 * KPIs/charts/leaderboard/people-summary a Director sees; those show what isn't theirs to
 * see. Executive and Super Admin share the exact same view as each other and as a Director
 * (per explicit request, reverting an earlier CEO-specific redesign) — the only difference
 * for that tier is the extra ExecutiveKpiRow up top and TopLevelTasksSection's org-wide
 * scope, both gated on isExecutive (true for Executive AND Super Admin).
 */
export function DashboardPage() {
  const { isDirector, isExecutive } = useAuth()

  if (!isDirector) {
    return (
      <>
        <PageHeader breadcrumb="Throughline" title="My Dashboard" />
        <MyDashboardSummary />
      </>
    )
  }

  return (
    <>
      <PageHeader breadcrumb="Throughline" title="Dashboard" />

      <div className={styles.kpiStack}>
        <KpiRow />
        {/* Departments-on-track / overdue / critical / awaiting-decision — Executive and
            Super Admin only, a plain Director doesn't get this row. */}
        {isExecutive && <ExecutiveKpiRow />}
      </div>

      <div className={styles.charts}>
        <Card>
          <div className={styles.sectionHeadingLg}>Progress over time</div>
          <ProgressOverTimeChart />
          <ProgressChartLegend />
        </Card>
        <Card>
          <div className={styles.sectionHeadingLg}>Status mix</div>
          <StatusDonut />
          <StatusDonutLegend />
        </Card>
      </div>

      {/* An Executive/Super Admin sees every CRITICAL task org-wide plus everything they
          personally assigned — the literal same view for both, not a separate lookalike. A
          plain Director gets the classic "my initiatives" scoping: only the top-level tasks
          they created themselves. */}
      <TopLevelTasksSection scope={isExecutive ? 'org-wide' : 'mine'} />

      <Card>
        <div className={styles.sectionHeadingLg}>Team leaderboard</div>
        <TeamLeaderboardTable />
      </Card>

      <div className={styles.peopleSection}>
        <div className={styles.sectionHeadingLg}>People summary</div>
        <PeopleSummaryGrid />
      </div>
    </>
  )
}
