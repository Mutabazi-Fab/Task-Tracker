import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import zigamaIcon from '../../../icon/Zigama icon.png'
import { Card } from '../../../components/ui/Card'
import styles from './AuthLayout.module.css'

interface AuthLayoutProps {
  title: string
  subtitle: string
  children: ReactNode
  footerText: string
  footerLinkTo: string
  footerLinkLabel: string
}

/** Shared chrome for LoginPage/SignupPage — logo, heading, form slot, and the link that
 *  switches between the two. Deliberately rendered outside AppShell (no sidebar, no
 *  global search bar) since there's no logged-in identity yet to build those around. */
export function AuthLayout({ title, subtitle, children, footerText, footerLinkTo, footerLinkLabel }: AuthLayoutProps) {
  return (
    <div className={styles.page}>
      <div className={styles.card}>
        <div className={styles.logoWrap}>
          <img src={zigamaIcon} alt="Zigama" className={styles.logo} />
        </div>

        <div className={styles.heading}>
          <span className={styles.title}>{title}</span>
          <span className={styles.subtitle}>{subtitle}</span>
        </div>

        <Card>{children}</Card>

        <div className={styles.footer}>
          {footerText} <Link to={footerLinkTo} className={styles.footerLink}>{footerLinkLabel}</Link>
        </div>
      </div>
    </div>
  )
}
