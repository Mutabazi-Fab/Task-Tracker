import { useEffect, useState, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import zigamaIcon from '../../../icon/Zigama icon.png'
import styles from './AuthLayout.module.css'

interface AuthLayoutProps {
  title: string
  subtitle: string
  children: ReactNode
  footerText: string
  footerLinkTo: string
  footerLinkLabel: string
}

/** What Throughline actually does, cycled on the branding side — not stock copy borrowed
 *  from an unrelated product. */
const SLIDES = [
  {
    heading: 'Every Task, Tracked End to End',
    body: "From a Director's top-level initiative down to each subtask, see exactly who owns what and how far it's come.",
  },
  {
    heading: 'Teams Accountable For Their Work',
    body: 'Every membership change, every reassignment, every percentage logged — recorded, attributed, and never lost.',
  },
  {
    heading: 'One Dashboard, The Full Chain',
    body: "A Director's initiatives roll up automatically from their teams' subtasks — no manual tallying, ever.",
  },
]

/**
 * Split-screen chrome for LoginPage/SignupPage — branding + rotating description on the
 * left, the form itself on the right. Rendered outside AppShell (no sidebar, no global
 * search bar) since there's no logged-in identity yet to build those around. The
 * branding side collapses away below ~900px so this doesn't break on a phone.
 */
export function AuthLayout({ title, subtitle, children, footerText, footerLinkTo, footerLinkLabel }: AuthLayoutProps) {
  const [slideIndex, setSlideIndex] = useState(0)

  useEffect(() => {
    const id = setInterval(() => setSlideIndex((i) => (i + 1) % SLIDES.length), 5000)
    return () => clearInterval(id)
  }, [])

  const slide = SLIDES[slideIndex]!

  return (
    <div className={styles.page}>
      <div className={styles.brandPanel}>
        <div className={styles.brandContent}>
          <div className={styles.logoBadge}>
            <img src={zigamaIcon} alt="Zigama" className={styles.logo} />
          </div>
          <h2 className={styles.slideHeading}>{slide.heading}</h2>
          <p className={styles.slideBody}>{slide.body}</p>
        </div>
        <div className={styles.dots} aria-hidden="true">
          {SLIDES.map((s, i) => (
            <span key={s.heading} className={i === slideIndex ? styles.dotActive : styles.dot} />
          ))}
        </div>
      </div>

      <div className={styles.formPanel}>
        <div className={styles.formCard}>
          <div className={styles.heading}>
            <span className={styles.title}>{title}</span>
            <span className={styles.subtitle}>{subtitle}</span>
          </div>

          {children}

          <div className={styles.footer}>
            {footerText} <Link to={footerLinkTo} className={styles.footerLink}>{footerLinkLabel}</Link>
          </div>
        </div>
      </div>
    </div>
  )
}
