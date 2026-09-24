import { useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { EmptyState } from '../../../components/ui/EmptyState'
import { AddDocumentModal } from './AddDocumentModal'
import { DocumentItem } from './DocumentItem'
import type { TaskDocument } from '../../../types/document.types'
import styles from './SubtasksPanel.module.css'

interface DocumentsPanelProps {
  taskId: number
  documents: TaskDocument[]
}

/** Supporting documents attached to this task — a memo, a directive, a spec, whatever's relevant
 *  alongside the task itself. */
export function DocumentsPanel({ taskId, documents }: DocumentsPanelProps) {
  const [addOpen, setAddOpen] = useState(false)

  return (
    <>
      <div className={styles.header}>
        <span>Supporting documents</span>
        <Button variant="primary" onClick={() => setAddOpen(true)}>
          Add document
        </Button>
      </div>

      {documents.length === 0 ? (
        <EmptyState title="No documents yet" description="Attach a memo, directive, or spec relevant to this task." />
      ) : (
        <div>
          {documents.map((document) => (
            <DocumentItem key={document.id} taskId={taskId} document={document} />
          ))}
        </div>
      )}

      <AddDocumentModal taskId={taskId} open={addOpen} onClose={() => setAddOpen(false)} />
    </>
  )
}
