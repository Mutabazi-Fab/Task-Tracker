import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { EmptyState } from '../../../components/ui/EmptyState'
import type { IncidentMonthlyCount } from '../../../types/incident.types'
import styles from './IncidentBreakdownChart.module.css'

const axisTick = { fontSize: 10, fontFamily: 'var(--font-mono)', fill: 'var(--muted)' }

/** Incidents per month — the one KPI the source Excel dashboard never surfaced despite
 *  tracking Date Occurred on every row. */
export function IncidentTrendChart({ data }: { data: IncidentMonthlyCount[] }) {
  if (data.length === 0) {
    return <EmptyState title="No incidents yet" />
  }

  return (
    <div className={styles.wrap}>
      <ResponsiveContainer width="100%" height={200}>
        <LineChart data={data} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
          <CartesianGrid stroke="var(--khaki)" strokeDasharray="3 3" vertical={false} />
          <XAxis dataKey="month" tick={axisTick} axisLine={{ stroke: 'var(--line)' }} tickLine={false} />
          <YAxis allowDecimals={false} tick={axisTick} axisLine={false} tickLine={false} width={28} />
          <Tooltip
            contentStyle={{
              background: 'var(--panel)',
              border: '1px solid var(--line)',
              borderRadius: 'var(--radius-md)',
              fontFamily: 'var(--font-sans)',
              fontSize: 12,
            }}
          />
          <Line type="monotone" dataKey="count" stroke="var(--accent)" strokeWidth={2} dot={{ r: 3 }} />
        </LineChart>
      </ResponsiveContainer>
    </div>
  )
}
