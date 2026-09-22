import { useState } from 'react'
import { TextField } from '../../../components/ui/TextField'
import { useAuth } from '../../auth/useAuth'
import { useAddSourceEntry } from '../hooks/useAddSourceEntry'
import { useSourceEntries } from '../hooks/useSourceEntries'
import type { TaskSource } from '../../../types/task.types'
import styles from './SourceDetailField.module.css'

interface SourceDetailFieldProps {
  source: TaskSource | ''
  value: string
  onChange: (value: string) => void
  label: string
  disabled?: boolean
}

/** The free-text "Source Detail" field, unchanged for anyone typing into it — nothing here
 *  restricts what can be entered. On top of that plain TextField: a row of clickable
 *  suggestion chips (previously-saved entries for this Source category), and — Executive/
 *  Super Admin only — a "+ Save" button that permanently adds whatever's currently typed as
 *  a new reusable suggestion, so the next person creating a task doesn't have to retype it. */
export function SourceDetailField({ source, value, onChange, label, disabled }: SourceDetailFieldProps) {
  const { currentUser, isExecutive } = useAuth()
  const entriesQuery = useSourceEntries(source)
  const addEntry = useAddSourceEntry()
  const [saved, setSaved] = useState(false)

  const entries = entriesQuery.data ?? []
  const trimmed = value.trim()
  const alreadySaved = entries.some((e) => e.label.toLowerCase() === trimmed.toLowerCase())
  const canSave = isExecutive && !disabled && trimmed !== '' && !alreadySaved

  function handleSave() {
    if (!canSave || !currentUser || source === '') return
    setSaved(false)
    addEntry.mutate(
      { source, label: trimmed, addedById: currentUser.id },
      { onSuccess: () => setSaved(true) },
    )
  }

  return (
    <div className={styles.wrap}>
      <div className={styles.row}>
        <div className={styles.field}>
          <TextField
            label={label}
            value={value}
            onChange={(v) => {
              onChange(v)
              setSaved(false)
            }}
            placeholder="e.g. Director Maj. Musoni, GPO, E&Y, Board of Directors"
            disabled={disabled}
          />
        </div>
        {isExecutive && !disabled && (
          <button
            type="button"
            className={styles.saveButton}
            onClick={handleSave}
            disabled={!canSave || addEntry.isPending}
            title="Save as a reusable source suggestion"
          >
            {addEntry.isPending ? '…' : '+ Save'}
          </button>
        )}
      </div>

      {saved && <span className={styles.savedNote}>Saved — it'll show up as a suggestion next time.</span>}

      {entries.length > 0 && (
        <div className={styles.chips}>
          {entries.map((entry) => (
            <button
              key={entry.id}
              type="button"
              className={styles.chip}
              disabled={disabled}
              onClick={() => onChange(entry.label)}
            >
              {entry.label}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
