import { Bar, BarChart, CartesianGrid, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { EmptyState } from '../../../components/ui/EmptyState'
import type { IncidentCount } from '../../../types/incident.types'
import { paletteColor } from '../lib/chartColors'
import styles from './IncidentBreakdownChart.module.css'

const axisTick = { fontSize: 10, fontFamily: 'var(--font-mono)', fill: 'var(--muted)' }

interface IncidentBreakdownChartProps {
  data: IncidentCount[]
  labelFor: (label: string) => string
  /** Per-bar colour keyed off the raw enum label (before labelFor formats it) — e.g.
   *  statusChartColor/severityChartColor, which reuse the exact hues their badges already
   *  use. Falls back to a rotating theme palette (paletteColor) when omitted, for charts
   *  with no inherent per-value colour (category). */
  colorFor?: (label: string) => string
}

/** A generic bar breakdown — reused for status, severity, and category, each with its own
 *  label formatter so raw enum constants never leak into the UI, and its own colour so bars
 *  are visually distinct instead of one flat colour. */
export function IncidentBreakdownChart({ data, labelFor, colorFor }: IncidentBreakdownChartProps) {
  const nonZero = data.filter((d) => d.count > 0)
  if (nonZero.length === 0) {
    return <EmptyState title="No incidents yet" />
  }

  const chartData = nonZero.map((d) => ({ ...d, displayLabel: labelFor(d.label) }))

  return (
    <div className={styles.wrap}>
      <ResponsiveContainer width="100%" height={Math.max(160, chartData.length * 32)}>
        <BarChart data={chartData} layout="vertical" margin={{ top: 4, right: 16, left: 0, bottom: 4 }}>
          <CartesianGrid stroke="var(--khaki)" strokeDasharray="3 3" horizontal={false} />
          <XAxis type="number" allowDecimals={false} tick={axisTick} axisLine={false} tickLine={false} />
          <YAxis type="category" dataKey="displayLabel" tick={axisTick} axisLine={false} tickLine={false} width={160} />
          <Tooltip
            contentStyle={{
              background: 'var(--panel)',
              border: '1px solid var(--line)',
              borderRadius: 'var(--radius-md)',
              fontFamily: 'var(--font-sans)',
              fontSize: 12,
            }}
          />
          <Bar dataKey="count" radius={[0, 4, 4, 0]}>
            {chartData.map((entry, index) => (
              <Cell key={entry.label} fill={colorFor ? colorFor(entry.label) : paletteColor(index)} />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  )
}
