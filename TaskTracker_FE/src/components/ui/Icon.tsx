import styles from './Icon.module.css'

/**
 * Inline SVG line icons — no icon font, no external package. Explicitly not
 * emoji: every glyph here is a stroke-based outline drawn at a fixed 24x24
 * viewBox, coloured via currentColor so it inherits whatever text colour
 * its container sets.
 */
export type IconName =
  | 'dashboard'
  | 'tasks'
  | 'people'
  | 'teams'
  | 'search'
  | 'check'
  | 'logout'
  | 'mail'
  | 'lock'
  | 'eye'
  | 'eyeOff'
  | 'shield'
  | 'bell'
  | 'chevronLeft'
  | 'chevronRight'

const PATHS: Record<IconName, string> = {
  dashboard: 'M4 4h6v7H4zM14 4h6v4h-6zM14 11h6v9h-6zM4 14h6v6H4z',
  tasks: 'M6 3.5h12a1 1 0 0 1 1 1V19a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1V4.5a1 1 0 0 1 1-1zM8.5 8h7M8.5 12h7M8.5 16h4.5',
  people: 'M12 11.5a3.75 3.75 0 1 0 0-7.5 3.75 3.75 0 0 0 0 7.5zM4.5 20a7.5 7.5 0 0 1 15 0',
  teams:
    'M8.5 10.5a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM17 10.5a2.75 2.75 0 1 0 0-5.5 2.75 2.75 0 0 0 0 5.5zM2.5 19.5a6 6 0 0 1 12 0M13 14.75a5.5 5.5 0 0 1 8.5 4.75',
  search: 'M11 4a7 7 0 1 0 0 14 7 7 0 0 0 0-14zM20.5 20.5l-4.3-4.3',
  check: 'M5 12.5l4.5 4.5L19 7.5',
  logout: 'M9 4H5.5a1 1 0 0 0-1 1v14a1 1 0 0 0 1 1H9M15.5 16l4-4-4-4M19 12H8.5',
  mail: 'M4 6h16a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V7a1 1 0 0 1 1-1zM3.5 7l8.5 6 8.5-6',
  lock: 'M6.5 10.5V8a5.5 5.5 0 0 1 11 0v2.5M5 10.5h14a1 1 0 0 1 1 1V19a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1v-7.5a1 1 0 0 1 1-1zM12 14v2.5',
  eye: 'M2 12s3.5-6.5 10-6.5S22 12 22 12s-3.5 6.5-10 6.5S2 12 2 12zM12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z',
  eyeOff:
    'M3.5 3.5l17 17M10.6 5.7A10.6 10.6 0 0 1 12 5.5c6.5 0 10 6.5 10 6.5a15.7 15.7 0 0 1-3.4 4.4M6.7 6.7C4 8.4 2 12 2 12s3.5 6.5 10 6.5a9.9 9.9 0 0 0 3.9-.8M9.5 9.9a3 3 0 0 0 4.2 4.2',
  shield: 'M12 3.5 5 6v6c0 4.5 3 7.5 7 8.5 4-1 7-4 7-8.5V6z M9 12l2 2 4-4.5',
  bell: 'M6 10.5a6 6 0 0 1 12 0v4.5l1.8 2.5H4.2L6 15z M10 20a2 2 0 0 0 4 0',
  chevronLeft: 'M15 5l-7 7 7 7',
  chevronRight: 'M9 5l7 7-7 7',
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
