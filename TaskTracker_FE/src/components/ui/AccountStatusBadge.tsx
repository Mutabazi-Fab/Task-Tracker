import styles from './AccountStatusBadge.module.css'

/** Active/Deactivated pill — shared between the People list, a profile header, and the
 *  admin controls that change it, so the same status always looks the same everywhere. */
export function AccountStatusBadge({ active }: { active: boolean }) {
  return (
    <span className={`${styles.badge} ${active ? styles.active : styles.inactive}`}>
      {active ? 'Active' : 'Deactivated'}
    </span>
  )
}
