import { useNavigate } from 'react-router-dom'
import { PageHeader } from '../../components/layout/PageHeader'
import { Card } from '../../components/ui/Card'
import { MANAGEMENT_ATTENTION_RULES, MINIMUM_COMPLETION_REQUIREMENTS, SCORE_SCALE, SEVERITY_TABLE } from './lib/guidanceReference'
import { GuidanceNotesSection } from './components/GuidanceNotesSection'
import styles from './IncidentGuidancePage.module.css'

/**
 * The one place a new Director or CEO reads before ever touching an incident. The top half
 * is fixed reference material carried over verbatim from the source Excel's "Lists &
 * Guidance" sheet — the same severity bands and closure checklist the system itself
 * enforces (see IncidentMapper.computeSeverity / IncidentServiceImpl.requireClosureReadiness)
 * — kept read-only here so it can never say something different from what the app actually
 * does. The bottom half (GuidanceNotesSection) is editable: anything org-specific that
 * changes over time (who to escalate to today, onboarding notes) belongs there instead.
 */
export function IncidentGuidancePage() {
  const navigate = useNavigate()

  return (
    <>
      <PageHeader breadcrumb="Throughline / Incident Management" title="Incident Guidance" onBack={() => navigate(-1)} />

      <Card>
        <div className={styles.sectionHeadingLg}>Severity & escalation</div>
        <p className={styles.intro}>
          Severity is never chosen directly — it's always Likelihood × Impact, banded per this table. This is exactly what
          drives the Severity badge you see on every incident.
        </p>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>Severity</th>
              <th>Score</th>
              <th>Indicative criteria</th>
              <th>Escalation</th>
              <th>Target initial report</th>
            </tr>
          </thead>
          <tbody>
            {SEVERITY_TABLE.map((row) => (
              <tr key={row.severity}>
                <td className={styles.severityCell}>{row.severity}</td>
                <td>{row.scoreRange}</td>
                <td>{row.criteria}</td>
                <td>{row.escalation}</td>
                <td>{row.targetInitialReport}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>

      <Card>
        <div className={styles.sectionHeadingLg}>Likelihood & impact scale</div>
        <p className={styles.intro}>What each 1-5 score means when you're scoring an incident.</p>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>Score</th>
              <th>Impact</th>
              <th>Likelihood</th>
            </tr>
          </thead>
          <tbody>
            {SCORE_SCALE.map((row) => (
              <tr key={row.score}>
                <td className={styles.severityCell}>{row.score}</td>
                <td>{row.impactMeaning}</td>
                <td>{row.likelihoodMeaning}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>

      <Card>
        <div className={styles.sectionHeadingLg}>Minimum completion requirements</div>
        <p className={styles.intro}>
          The system enforces the two starred by refusing to close an incident without them (plus Risk/Compliance review
          for Critical/High or regulator-notifiable incidents) — the rest are process discipline, not a system gate.
        </p>
        <div className={styles.checklist}>
          {MINIMUM_COMPLETION_REQUIREMENTS.map((item) => (
            <div key={item.title} className={styles.checklistItem}>
              <span className={styles.checklistTitle}>{item.title}</span>
              <span className={styles.checklistDescription}>{item.description}</span>
            </div>
          ))}
        </div>
      </Card>

      <Card>
        <div className={styles.sectionHeadingLg}>Management attention rules</div>
        <p className={styles.attentionRules}>{MANAGEMENT_ATTENTION_RULES}</p>
      </Card>

      <Card>
        <div className={styles.sectionHeadingLg}>Guidance notes</div>
        <p className={styles.intro}>
          Anything beyond the fixed material above — current escalation contacts, onboarding notes for a new Director or
          CEO — goes here. Director/Executive/Super Admin can add, edit, or remove notes.
        </p>
        <GuidanceNotesSection />
      </Card>
    </>
  )
}
