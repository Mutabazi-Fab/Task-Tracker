import styles from './ProgressSlider.module.css'

const TICKS = [0, 25, 50, 75, 100]

/** Only applied once you let go (see handleRelease) — snapping this live, on every value
 *  as you drag, used to swallow a whole band of values around each tick (e.g. dragging
 *  through 22–28 all displayed as 25, then jumped straight to 29), making it impossible to
 *  land on or move smoothly through anything near a tick. Kept small so it's a subtle
 *  "close enough" correction at the end of a drag, not something that fights the drag
 *  itself. */
const SNAP_THRESHOLD = 2

interface ProgressSliderProps {
  value: number
  onChange: (value: number) => void
  disabled?: boolean
}

/**
 * A single draggable bar from 0 to 100 — replaces the old step-buttons + exact-percentage
 * text field. Reference ticks at 0/25/50/75/100 mark the track (see the module's CSS for
 * the "0% ── 25% ── 50% ── ... ── 100%" layout). While dragging, the value tracks the
 * pointer exactly — every value from 0 to 100 is reachable, one at a time, with no
 * interference. Only on release does it snap to a reference tick, and only if you're
 * already within SNAP_THRESHOLD of one. Built on a native `<input type="range">` for free
 * keyboard/touch/pointer support and accessibility — the ticks and value bubble are purely
 * visual, layered on top via absolute positioning.
 */
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
        // Live, exact, no snapping — this is what makes every value 0–100 reachable one
        // at a time while actually dragging.
        onChange={(e) => onChange(Number(e.target.value))}
        // Snap-to-tick only fires once the drag/keypress ends, and only nudges the value
        // if it's already close to a tick — see handleRelease and SNAP_THRESHOLD above.
        onMouseUp={(e) => handleRelease(Number(e.currentTarget.value))}
        onTouchEnd={(e) => handleRelease(Number(e.currentTarget.value))}
        onKeyUp={(e) => handleRelease(Number(e.currentTarget.value))}
        // The traversed portion of the track (0 up to the current value) fills in green as
        // you drag — --fill is read by the track styling below in ProgressSlider.module.css.
        // Set as an inline custom property (not a plain style prop) so it also reaches the
        // ::-webkit-slider-runnable-track pseudo-element, which inline `style` can't target
        // directly.
        style={{ ['--fill' as string]: `${value}%` }}
      />

      <div className={styles.ticks}>
        {TICKS.map((tick) => (
          <span
            key={tick}
            className={styles.tick}
            // Centered on every tick except the two ends, which would otherwise overflow
            // the track's own width.
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
