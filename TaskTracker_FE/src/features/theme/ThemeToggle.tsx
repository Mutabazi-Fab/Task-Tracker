import { useId } from 'react'
import { useTheme } from './useTheme'
import styles from './ThemeToggle.module.css'

/** Filled, not stroke-outline — a deliberate one-off so this control stands
 * out the way a sun/moon theme switch does in most apps, rather than
 * blending into the rest of the outline icon set. */
function SunIcon({ size = 16 }: { size?: number }) {
  return (
    <svg viewBox="0 0 24 24" width={size} height={size} fill="currentColor" aria-hidden="true">
      <circle cx="12" cy="12" r="4.5" />
      {[0, 45, 90, 135, 180, 225, 270, 315].map((deg) => (
        <rect key={deg} x="11.1" y="1.5" width="1.8" height="4" rx="0.9" transform={`rotate(${deg} 12 12)`} />
      ))}
    </svg>
  )
}

/**
 * Built from two overlapping circles via a mask, not a hand-tuned crescent path — the
 * previous single-path crescent had a near-tangent point where its two arcs met, which
 * rendered as a thin stray sliver ("a shadow") at small icon sizes once filled solid.
 * Two generously-sized, moderately-offset circles keep the crescent evenly thick
 * end to end with no needle point.
 */
function MoonIcon({ size = 16 }: { size?: number }) {
  const maskId = useId()
  return (
    <svg viewBox="0 0 24 24" width={size} height={size} aria-hidden="true">
      <mask id={maskId}>
        <rect x="0" y="0" width="24" height="24" fill="white" />
        <circle cx="15.5" cy="8.5" r="7" fill="black" />
      </mask>
      <circle cx="12" cy="12" r="9" fill="currentColor" mask={`url(#${maskId})`} />
    </svg>
  )
}

/**
 * A single small sliding switch, not two full-size buttons side by side — Field/Command
 * is one binary choice, so one control that flips is both more compact and a clearer fit
 * for what it actually is than a pair of tabs. The sliding thumb keeps the same colours
 * as before (accent-strong fill, panel-coloured icon); the theme not currently active
 * shows dimly in the track next to it, in the same muted tone used everywhere else.
 */
export function ThemeToggle() {
  const { theme, setTheme } = useTheme()
  const isCommand = theme === 'command'

  return (
    <button
      type="button"
      role="switch"
      aria-checked={isCommand}
      aria-label="Toggle theme"
      className={styles.track}
      onClick={() => setTheme(isCommand ? 'field' : 'command')}
    >
      <span className={styles.iconSun}>
        <SunIcon size={13} />
      </span>
      <span className={styles.iconMoon}>
        <MoonIcon size={13} />
      </span>
      <span className={isCommand ? styles.thumbCommand : styles.thumb}>
        {isCommand ? <MoonIcon size={15} /> : <SunIcon size={15} />}
      </span>
    </button>
  )
}
