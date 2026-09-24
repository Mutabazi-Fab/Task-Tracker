import { PageHeader } from '../../components/layout/PageHeader'
import { Card } from '../../components/ui/Card'
import { useAuth } from '../auth/useAuth'
import { TopLevelTasksSection } from './components/TopLevelTasksSection'
import { DirectorTasksPanel } from './components/DirectorTasksPanel'
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

/** Composes the sections below. */
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

      {/* An Executive/Super Admin sees every CRITICAL task org-wide plus everything they personally assigned,
         in one panel — the literal same view for both, not a separate lookalike. */}
      {isExecutive ? <TopLevelTasksSection scope="org-wide" /> : <DirectorTasksPanel />}

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
