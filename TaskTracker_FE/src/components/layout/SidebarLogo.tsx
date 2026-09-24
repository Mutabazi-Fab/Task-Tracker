import zigamaIcon from '../../icon/Zigama icon.jpeg'
import styles from './SidebarLogo.module.css'

/** The full Zigama badge, mark plus "ZIGAMA CSS" wordmark — sits at the top of the Sidebar, sized to
 *  show the whole thing (see .mark: height auto, no cropping). */
export function SidebarLogo() {
  return (
    <div className={styles.wrap}>
      <img src={zigamaIcon} alt="Zigama" className={styles.mark} />
    </div>
  )
}
