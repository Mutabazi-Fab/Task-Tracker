import { Area, AreaChart, ResponsiveContainer, Tooltip } from 'recharts'
import { formatDateTime } from '../../../lib/formatDate'
import { formatPercentage } from '../../../lib/formatPercentage'
import type { TaskTimelinePoint } from '../../../types/task.types'
import styles from './TaskProgressSparkline.module.css'

/** This one task's percentage over its own comment history — no axes, just the shape. */
export function TaskProgressSparkline({ points }: { points: TaskTimelinePoint[] }) {
  if (points.length < 2) return null

  return (
    <div className={styles.wrap}>
      <ResponsiveContainer width="100%" height={64}>
        <AreaChart data={points} margin={{ top: 4, right: 4, left: 4, bottom: 0 }}>
          <Tooltip
            labelFormatter={(_label, payload) => {
              const point = payload?.[0]?.payload as TaskTimelinePoint | undefined
              return point ? formatDateTime(point.date) : ''
            }}
            formatter={(value) => [formatPercentage(Number(value)), 'Progress']}
            contentStyle={{
              background: 'var(--panel)',
              border: '1px solid var(--line)',
              borderRadius: 'var(--radius-md)',
              fontFamily: 'var(--font-sans)',
              fontSize: 12,
            }}
          />
          <Area type="monotone" dataKey="percentage" stroke="var(--accent)" fill="var(--accent-soft)" strokeWidth={2} />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  )
}
