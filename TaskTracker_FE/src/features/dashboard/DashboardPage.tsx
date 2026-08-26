import { useState } from 'react'
import { PageHeader } from '../../components/layout/PageHeader'
import { Card } from '../../components/ui/Card'
import { KpiRow } from './components/KpiRow'
import { ProgressOverTimeChart } from './components/ProgressOverTimeChart'
import { ProgressChartLegend } from './components/ProgressChartLegend'
import { StatusDonut } from './components/StatusDonut'
import { StatusDonutLegend } from './components/StatusDonutLegend'
import { TeamLeaderboardTable } from './components/TeamLeaderboardTable'
import { PeopleSummaryGrid } from './components/PeopleSummaryGrid'
import { DashboardLayoutToggle, type DashboardLayout } from './components/DashboardLayoutToggle'
import styles from './DashboardPage.module.css'

/** Composes the sections below. No data-fetching or layout logic of its own. */
export function DashboardPage() {
  const [layout, setLayout] = useState<DashboardLayout>('split')

  return (
    <>
      <PageHeader
        breadcrumb="Throughline"
        title="Dashboard"
        right={<DashboardLayoutToggle value={layout} onChange={setLayout} />}
      />

      <KpiRow />

      <div className={layout === 'split' ? styles.chartsSplit : styles.chartsTrendFocus}>
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
