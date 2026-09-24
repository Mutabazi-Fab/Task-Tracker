import { useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { EmptyState } from '../../../components/ui/EmptyState'
import { ErrorMessage } from '../../../components/ui/ErrorMessage'
import { QueryBoundary } from '../../../components/feedback/QueryBoundary'
import { formatDateTime } from '../../../lib/formatDate'
import { useAuth } from '../../auth/useAuth'
import { useGuidanceNotes } from '../hooks/useGuidanceNotes'
import { useCreateGuidanceNote, useDeleteGuidanceNote, useUpdateGuidanceNote } from '../hooks/useGuidanceNoteMutations'
import { GuidanceNoteForm } from './GuidanceNoteForm'
import type { GuidanceNote } from '../../../types/incidentGuidance.types'
import styles from './GuidanceNotesSection.module.css'

function NoteEditForm({ note, onDone }: { note: GuidanceNote; onDone: () => void }) {
  const { currentUser } = useAuth()
  const updateNote = useUpdateGuidanceNote(note.id)

  async function handleSubmit(title: string, body: string) {
    await updateNote.mutateAsync({ title, body, changedById: currentUser?.id ?? 0 })
    onDone()
  }

  return (
    <>
      {updateNote.isError && <ErrorMessage message={updateNote.error.message} />}
      <GuidanceNoteForm
        initialTitle={note.title}
        initialBody={note.body}
        onSubmit={handleSubmit}
        onCancel={onDone}
        submitting={updateNote.isPending}
        submitLabel="Save"
      />
    </>
  )
}

function NoteCard({ note }: { note: GuidanceNote }) {
  const { isDirector } = useAuth()
  const [editing, setEditing] = useState(false)
  const deleteNote = useDeleteGuidanceNote()

  if (editing) {
    return (
      <div className={styles.note}>
        <NoteEditForm note={note} onDone={() => setEditing(false)} />
      </div>
    )
  }

  return (
    <div className={styles.note}>
      <div className={styles.noteHeader}>
        <span className={styles.noteTitle}>{note.title}</span>
        {isDirector && (
          <div className={styles.noteActions}>
            <Button type="button" variant="ghost" onClick={() => setEditing(true)}>
              Edit
            </Button>
            <Button type="button" variant="danger" onClick={() => deleteNote.mutate(note.id)} disabled={deleteNote.isPending}>
              Delete
            </Button>
          </div>
        )}
      </div>
      <p className={styles.noteBody}>{note.body}</p>
      <div className={styles.noteMeta}>
        {note.updatedByName
          ? `Last updated by ${note.updatedByName} · ${formatDateTime(note.updatedAt)}`
          : `Added by ${note.createdByName} · ${formatDateTime(note.createdAt)}`}
      </div>
    </div>
  )
}

/** The editable half of the Guidance page — free-text notes any Director/Executive/Super
 *  Admin can add, edit, or remove, meant for whatever the fixed Excel reference material
 *  (severity table, score scale, completion checklist — shown above this section) doesn't
 *  cover: escalation contacts changing, org-specific onboarding notes for a new Director or
 *  CEO, anything current at the time rather than fixed at launch. */
export function GuidanceNotesSection() {
  const { isDirector, currentUser } = useAuth()
  const query = useGuidanceNotes()
  const createNote = useCreateGuidanceNote()
  const [adding, setAdding] = useState(false)

  async function handleCreate(title: string, body: string) {
    await createNote.mutateAsync({ title, body, createdById: currentUser?.id ?? 0 })
    setAdding(false)
  }

  return (
    <div>
      <QueryBoundary query={query}>
        {(notes) =>
          notes.length === 0 && !adding ? (
            <EmptyState title="No guidance notes yet" description="Add one to help the next new Director or CEO get oriented." />
          ) : (
            <div className={styles.list}>
              {notes.map((note) => (
                <NoteCard key={note.id} note={note} />
              ))}
            </div>
          )
        }
      </QueryBoundary>

      {isDirector && (
        <>
          {createNote.isError && <ErrorMessage message={createNote.error.message} />}
          {adding ? (
            <GuidanceNoteForm onSubmit={handleCreate} onCancel={() => setAdding(false)} submitting={createNote.isPending} submitLabel="Add note" />
          ) : (
            <div className={styles.addRow}>
              <Button type="button" variant="secondary" onClick={() => setAdding(true)}>
                Add guidance note
              </Button>
            </div>
          )}
        </>
      )}
    </div>
  )
}
