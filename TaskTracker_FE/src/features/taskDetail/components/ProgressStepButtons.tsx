import styles from './ProgressStepButtons.module.css'

const STEPS = [0, 25, 50, 75, 100]

interface ProgressStepButtonsProps {
  value: number | null
  onChange: (value: number) => void
}

/**
 * Quick-set percentage. Only fills the percentage field — AddCommentForm
 * still requires body text before it can submit, so a step alone never
 * changes progress.
 */
export function ProgressStepButtons({ value, onChange }: ProgressStepButtonsProps) {
  return (
    <div className={styles.row}>
      {STEPS.map((step) => (
        <button
          key={step}
          type="button"
          className={step === value ? styles.stepActive : styles.step}
          onClick={() => onChange(step)}
        >
          {step}%
        </button>
      ))}
    </div>
  )
}
