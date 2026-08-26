import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { QueryBoundary } from '../../../components/feedback/QueryBoundary'
import { EmptyState } from '../../../components/ui/EmptyState'
import { formatDate } from '../../../lib/formatDate'
import { formatPercentage } from '../../../lib/formatPercentage'
import { useProgressOverTime } from '../hooks/useProgressOverTime'
import styles from './ProgressOverTimeChart.module.css'

const axisTick = { fontSize: 10, fontFamily: 'var(--font-mono)', fill: 'var(--muted)' }

/** The org-wide progress trend — a LINE (area) chart across dates, never the donut's job. */
export function ProgressOverTimeChart() {
  const query = useProgressOverTime()

  return (
    <QueryBoundary query={query}>
      {(points) =>
        points.length === 0 ? (
          <EmptyState title="No progress history yet" description="Points appear once tasks have comments in this range." />
        ) : (
          <div className={styles.wrap}>
            <ResponsiveContainer width="100%" height={220}>
              <AreaChart data={points} margin={{ top: 8, right: 8, left: -16, bottom: 0 }}>
                <CartesianGrid stroke="var(--khaki)" strokeDasharray="3 3" vertical={false} />
                <XAxis
                  dataKey="date"
                  tickFormatter={(value: string) => formatDate(value)}
                  tick={axisTick}
                  axisLine={{ stroke: 'var(--line)' }}
                  tickLine={false}
                  minTickGap={24}
                />
                <YAxis domain={[0, 100]} tick={axisTick} axisLine={false} tickLine={false} width={32} />
                <Tooltip
                  labelFormatter={(label) => formatDate(String(label))}
                  formatter={(value) => [formatPercentage(Number(value)), 'Avg progress']}
                  contentStyle={{
                    background: 'var(--panel)',
                    border: '1px solid var(--line)',
                    borderRadius: 'var(--radius-md)',
                    fontFamily: 'var(--font-sans)',
                    fontSize: 12,
                  }}
                />
                <Area
                  type="monotone"
                  dataKey="averagePercentage"
                  stroke="var(--accent)"
                  fill="var(--accent-soft)"
                  strokeWidth={2}
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        )
      }
    </QueryBoundary>
  )
}
