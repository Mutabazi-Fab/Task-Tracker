import zigamaIcon from '../../icon/Zigama icon.png'
import styles from './SidebarLogo.module.css'

/** The Zigama mark, icon-only — no wordmark. Sits at the top of the Sidebar. */
export function SidebarLogo() {
  return (
    <div className={styles.wrap}>
      <img src={zigamaIcon} alt="Zigama" className={styles.mark} />
    </div>
  )
}
