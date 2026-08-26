import type { Team } from '../../../types/team.types'
import styles from './TeamHeader.module.css'

/** Name + team leader. */
export function TeamHeader({ team }: { team: Team }) {
  return (
    <div className={styles.wrap}>
      <h2 className={styles.name}>{team.name}</h2>
      <span className={styles.leader}>{team.leaderName ? `Led by ${team.leaderName}` : 'No leader assigned'}</span>
    </div>
  )
}
