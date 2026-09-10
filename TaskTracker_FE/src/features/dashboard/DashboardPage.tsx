import { PageHeader } from '../../components/layout/PageHeader'
import { Card } from '../../components/ui/Card'
import { useAuth } from '../auth/useAuth'
import { TopLevelTasksSection } from './components/TopLevelTasksSection'
import { MyDashboardSummary } from './components/MyDashboardSummary'
import { KpiRow } from './components/KpiRow'
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
 * KPIs/charts/leaderboard/people-summary a Director/Super Admin sees; those show what
 * isn't theirs to see.
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

      <KpiRow />

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
          personally assigned — the literal same view for both, not a separate lookalike,
          and not scoped to top-level depth (see TopLevelTasksSection's own doc comment). A
          plain Director still gets the classic "my initiatives" scoping: only the top-level
          tasks they created themselves. */}
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
