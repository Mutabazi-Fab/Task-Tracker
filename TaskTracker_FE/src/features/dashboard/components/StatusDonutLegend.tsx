import { QueryBoundary } from '../../../components/feedback/QueryBoundary'
import { StatusChip } from '../../../components/ui/StatusChip'
import { formatPercentage } from '../../../lib/formatPercentage'
import { useStatusMix } from '../hooks/useStatusMix'
import styles from './StatusDonutLegend.module.css'

/**
 * Renders via StatusChip rather than its own colour swatches — StatusChip,
 * ProgressBar and the donut are the only things allowed to render status
 * colour, so the legend borrows the chip instead of inventing a new swatch.
 */
export function StatusDonutLegend() {
  const query = useStatusMix()

  return (
    <QueryBoundary query={query}>
      {(mix) => (
        <div className={styles.legend}>
          {mix.map((slice) => (
            <div key={slice.status} className={styles.row}>
              <StatusChip status={slice.status} />
              <span className={styles.count}>{slice.count}</span>
              <span className={styles.share}>{formatPercentage(slice.percentageShare)}</span>
            </div>
          ))}
        </div>
      )}
    </QueryBoundary>
  )
}
