import { PageHeader } from '../../components/layout/PageHeader'
import { Card } from '../../components/ui/Card'
import { useAuth } from '../auth/useAuth'
import { DirectorInitiativesSection } from './components/DirectorInitiativesSection'
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
  const { isDirector } = useAuth()

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
          <div className={styles.sectionHeading}>Progress over time</div>
          <ProgressOverTimeChart />
          <ProgressChartLegend />
        </Card>
        <Card>
          <div className={styles.sectionHeading}>Status mix</div>
          <StatusDonut />
          <StatusDonutLegend />
        </Card>
      </div>

      <DirectorInitiativesSection />

      <Card>
        <div className={styles.sectionHeading}>Team leaderboard</div>
        <TeamLeaderboardTable />
      </Card>

      <div className={styles.peopleSection}>
        <div className={styles.sectionHeading}>People summary</div>
        <PeopleSummaryGrid />
      </div>
    </>
  )
}
