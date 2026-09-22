import styles from './ProgressSlider.module.css'

const TICKS = [0, 25, 50, 75, 100]

/** Only applied on release (see handleRelease) — snapping live while dragging used to
 *  swallow a whole band of values around each tick, making it impossible to land on
 *  anything near one. Kept small so it's a subtle correction, not a fight with the drag. */
const SNAP_THRESHOLD = 2

interface ProgressSliderProps {
  value: number
  onChange: (value: number) => void
  disabled?: boolean
}

/** A single draggable bar from 0 to 100, with reference ticks at 0/25/50/75/100. While
 *  dragging, every value is reachable exactly; only on release does it snap to a nearby
 *  tick (see SNAP_THRESHOLD). Built on a native `<input type="range">` for free keyboard/
 *  touch/pointer support — the ticks and value bubble are purely visual overlays. */
export function ProgressSlider({ value, onChange, disabled }: ProgressSliderProps) {
  function handleRelease(current: number) {
    const nearestTick = TICKS.reduce((closest, tick) => (Math.abs(tick - current) < Math.abs(closest - current) ? tick : closest));
    if (Math.abs(nearestTick - current) <= SNAP_THRESHOLD && nearestTick !== current) {
      onChange(nearestTick);
    }
  }

  return (
    <div className={styles.wrap}>
      <div className={styles.bubbleRow}>
        <span
          className={styles.bubble}
          style={{
            left: `${value}%`,
            transform: value <= 2 ? 'none' : value >= 98 ? 'translateX(-100%)' : 'translateX(-50%)',
          }}
        >
          {value}%
        </span>
      </div>

      <input
        type="range"
        className={styles.range}
        min={0}
        max={100}
        step={1}
        value={value}
        disabled={disabled}
        // Live, exact, no snapping while dragging.
        onChange={(e) => onChange(Number(e.target.value))}
        // Snap-to-tick fires once the drag/keypress ends — see handleRelease/SNAP_THRESHOLD.
        onMouseUp={(e) => handleRelease(Number(e.currentTarget.value))}
        onTouchEnd={(e) => handleRelease(Number(e.currentTarget.value))}
        onKeyUp={(e) => handleRelease(Number(e.currentTarget.value))}
        // --fill (read by ProgressSlider.module.css) as an inline custom property, not a
        // plain style prop, so it also reaches ::-webkit-slider-runnable-track.
        style={{ ['--fill' as string]: `${value}%` }}
      />

      <div className={styles.ticks}>
        {TICKS.map((tick) => (
          <span
            key={tick}
            className={styles.tick}
            // Centered except at the two ends, which would otherwise overflow the track.
            style={{ left: `${tick}%`, transform: tick === 0 ? 'none' : tick === 100 ? 'translateX(-100%)' : 'translateX(-50%)' }}
          >
            <span className={styles.tickMark} />
            <span className={styles.tickLabel}>{tick}%</span>
          </span>
        ))}
      </div>
    </div>
  )
}
