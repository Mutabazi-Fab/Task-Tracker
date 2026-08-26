import styles from './Icon.module.css'

/**
 * Inline SVG line icons — no icon font, no external package. Explicitly not
 * emoji: every glyph here is a stroke-based outline drawn at a fixed 24x24
 * viewBox, coloured via currentColor so it inherits whatever text colour
 * its container sets.
 */
export type IconName = 'dashboard' | 'tasks' | 'people' | 'teams' | 'search' | 'check'

const PATHS: Record<IconName, string> = {
  dashboard: 'M4 4h6v7H4zM14 4h6v4h-6zM14 11h6v9h-6zM4 14h6v6H4z',
  tasks: 'M6 3.5h12a1 1 0 0 1 1 1V19a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1V4.5a1 1 0 0 1 1-1zM8.5 8h7M8.5 12h7M8.5 16h4.5',
  people: 'M12 11.5a3.75 3.75 0 1 0 0-7.5 3.75 3.75 0 0 0 0 7.5zM4.5 20a7.5 7.5 0 0 1 15 0',
  teams:
    'M8.5 10.5a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM17 10.5a2.75 2.75 0 1 0 0-5.5 2.75 2.75 0 0 0 0 5.5zM2.5 19.5a6 6 0 0 1 12 0M13 14.75a5.5 5.5 0 0 1 8.5 4.75',
  search: 'M11 4a7 7 0 1 0 0 14 7 7 0 0 0 0-14zM20.5 20.5l-4.3-4.3',
  check: 'M5 12.5l4.5 4.5L19 7.5',
}

interface IconProps {
  name: IconName
  size?: number
}

export function Icon({ name, size = 16 }: IconProps) {
  return (
    <svg
      className={styles.icon}
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.8}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d={PATHS[name]} />
    </svg>
  )
}
