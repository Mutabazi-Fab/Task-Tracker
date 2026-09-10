import type { Team } from '../../../types/team.types'
import styles from './TeamHeader.module.css'

/** Name + team leader + the Department this team belongs to. */
export function TeamHeader({ team }: { team: Team }) {
  return (
    <div className={styles.wrap}>
      <h2 className={styles.name}>{team.name}</h2>
      <span className={styles.leader}>{team.leaderName ? `Led by ${team.leaderName}` : 'No leader assigned'}</span>
      {team.departmentName && <span className={styles.leader}>Department: {team.departmentName}</span>}
    </div>
  )
}
