import { PageHeader } from '../../components/layout/PageHeader'
import { Card } from '../../components/ui/Card'
import { useAuth } from '../auth/useAuth'
import { TopLevelTasksSection } from './components/TopLevelTasksSection'
import { MyDashboardSummary } from './components/MyDashboardSummary'
import { KpiRow } from './components/KpiRow'
import { ExecutiveKpiRow } from './components/ExecutiveKpiRow'
import { DepartmentHealthTable } from './components/DepartmentHealthTable'
import { PendingDecisionsPanel } from './components/PendingDecisionsPanel'
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
 * see. A plain Executive (not Super Admin) gets its own exception-based-management layout
 * below that — org-health KPIs, a per-department roll-up instead of individual task cards,
 * pending decisions surfaced up top, Leaderboard/People-summary pushed lower. Super Admin is
 * deliberately excluded from that branch and falls through to the classic Director-shaped
 * stack instead (per explicit request — the redesign is CEO-specific, Super Admin keeps its
 * original org-wide operational view).
 */
export function DashboardPage() {
  const { isDirector, isExecutive, isSuperAdmin } = useAuth()

  if (!isDirector) {
    return (
      <>
        <PageHeader breadcrumb="Throughline" title="My Dashboard" />
        <MyDashboardSummary />
      </>
    )
  }

  if (isExecutive && !isSuperAdmin) {
    return (
      <>
        <PageHeader breadcrumb="Throughline" title="Dashboard" />

        <ExecutiveKpiRow />

        <Card>
          <div className={styles.sectionHeadingLg}>Department health</div>
          <DepartmentHealthTable />
        </Card>

        <PendingDecisionsPanel />

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

      {/* Only a plain Director or Super Admin reach here (a plain Executive is diverted to
          the branch above). Super Admin still sees every CRITICAL task org-wide plus
          everything an Executive/Super Admin personally assigned — the original behaviour,
          restored on request. A plain Director gets the classic "my initiatives" scoping:
          only the top-level tasks they created themselves. */}
      <TopLevelTasksSection scope={isSuperAdmin ? 'org-wide' : 'mine'} />

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
