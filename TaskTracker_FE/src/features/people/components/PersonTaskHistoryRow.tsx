import { Link } from 'react-router-dom'
import { ROUTES } from '../../../app/routes'
import type { PersonTaskHistoryItem } from '../../../types/person.types'
import styles from './PersonTaskHistoryRow.module.css'

const LABEL_TEXT: Record<PersonTaskHistoryItem['involvementLabel'], string> = {
  CURRENT_OWNER: 'Current owner',
  VIA_TEAM: 'Via team',
  PREVIOUSLY_ASSIGNED: 'Previously assigned',
  COMMENTER_ONLY: 'Commenter only',
  UNKNOWN: 'Unknown',
}

/** Includes the involvement label — a neutral badge, not a status colour. */
export function PersonTaskHistoryRow({ item }: { item: PersonTaskHistoryItem }) {
  return (
    <Link to={ROUTES.taskDetail(item.taskId)} className={styles.row}>
      <span className={styles.code}>{item.taskCode}</span>
      <span className={styles.title}>{item.title}</span>
      <span className={styles.badge}>{LABEL_TEXT[item.involvementLabel]}</span>
    </Link>
  )
}
