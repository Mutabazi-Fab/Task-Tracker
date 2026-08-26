import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts'
import { QueryBoundary } from '../../../components/feedback/QueryBoundary'
import { EmptyState } from '../../../components/ui/EmptyState'
import { statusColorKey } from '../../../lib/statusColor'
import type { StatusMix } from '../../../types/dashboard.types'
import { useStatusMix } from '../hooks/useStatusMix'
import styles from './StatusDonut.module.css'

const SLICE_FILL: Record<ReturnType<typeof statusColorKey>, string> = {
  completed: 'var(--status-completed)',
  ongoing: 'var(--status-ongoing)',
  pending: 'var(--status-pending)',
}

/** Status mix at today's date. Uses exactly the three status colours, no others. */
export function StatusDonut() {
  const query = useStatusMix()

  return (
    <QueryBoundary query={query}>
      {(mix) =>
        mix.length === 0 || mix.every((slice) => slice.count === 0) ? (
          <EmptyState title="No tasks yet" />
        ) : (
          <div className={styles.wrap}>
            <ResponsiveContainer width="100%" height={200}>
              <PieChart>
                <Pie
                  data={mix}
                  dataKey="count"
                  nameKey="status"
                  innerRadius={60}
                  outerRadius={88}
                  paddingAngle={2}
                  strokeWidth={0}
                >
                  {mix.map((slice) => (
                    <Cell key={slice.status} fill={SLICE_FILL[statusColorKey(slice.status)]} />
                  ))}
                </Pie>
                <Tooltip
                  formatter={(value, _name, entry) => [`${value} tasks`, (entry.payload as StatusMix).status]}
                  contentStyle={{
                    background: 'var(--panel)',
                    border: '1px solid var(--line)',
                    borderRadius: 'var(--radius-md)',
                    fontFamily: 'var(--font-sans)',
                    fontSize: 12,
                  }}
                />
              </PieChart>
            </ResponsiveContainer>
          </div>
        )
      }
    </QueryBoundary>
  )
}
