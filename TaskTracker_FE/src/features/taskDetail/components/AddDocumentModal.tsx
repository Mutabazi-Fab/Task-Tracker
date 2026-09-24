import { useState } from 'react'
import { Modal } from '../../../components/ui/Modal'
import { Button } from '../../../components/ui/Button'
import { ErrorMessage } from '../../../components/ui/ErrorMessage'
import { useAddDocument } from '../hooks/useAddDocument'
import styles from './ReassignTaskModal.module.css'

const MAX_SIZE_BYTES = 20 * 1024 * 1024

// Mirrors TaskServiceImpl.ALLOWED_DOCUMENT_CONTENT_TYPES — UX only, the backend is
// authoritative and rejects anything outside this list regardless of what the browser sends.
const ALLOWED_CONTENT_TYPES = new Set([
  'application/pdf',
  'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'application/vnd.ms-excel',
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  'application/vnd.ms-powerpoint',
  'application/vnd.openxmlformats-officedocument.presentationml.presentation',
  'image/png',
  'image/jpeg',
  'text/plain',
])

interface AddDocumentModalProps {
  taskId: number
  open: boolean
  onClose: () => void
}

/** A supporting document attached directly to an already-existing task — the other half of attaching
 *  one at creation time (see CreateTaskForm/CreateSubtaskForm). */
export function AddDocumentModal({ taskId, open, onClose }: AddDocumentModalProps) {
  const [file, setFile] = useState<File | null>(null)
  const [clientError, setClientError] = useState<string | null>(null)
  const addDocument = useAddDocument(taskId)

  function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const selected = e.target.files?.[0] ?? null
    setClientError(null)
    if (!selected) {
      setFile(null)
      return
    }
    if (selected.size > MAX_SIZE_BYTES) {
      setClientError("That file is larger than 20MB — it won't be accepted.")
      setFile(null)
      return
    }
    if (!ALLOWED_CONTENT_TYPES.has(selected.type)) {
      setClientError('Unsupported file type — PDF, Word, Excel, PowerPoint, PNG/JPEG, or plain text only.')
      setFile(null)
      return
    }
    setFile(selected)
  }

  function handleClose() {
    setFile(null)
    setClientError(null)
    onClose()
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!file) return
    addDocument.mutate(file, { onSuccess: handleClose })
  }

  return (
    <Modal open={open} onClose={handleClose} title="Add a supporting document">
      <form className={styles.form} onSubmit={handleSubmit}>
        <div className={styles.field}>
          <span className={styles.label}>File</span>
          <input type="file" onChange={handleFileChange} />
        </div>
        {clientError && <ErrorMessage message={clientError} />}
        {addDocument.isError && <ErrorMessage message={addDocument.error.message} />}

        <div className={styles.actions}>
          <Button type="button" variant="ghost" onClick={handleClose} disabled={addDocument.isPending}>
            Cancel
          </Button>
          <Button type="submit" disabled={!file || addDocument.isPending}>
            {addDocument.isPending ? 'Uploading…' : 'Upload'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
