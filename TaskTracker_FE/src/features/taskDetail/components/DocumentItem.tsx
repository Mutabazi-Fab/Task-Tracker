import { useState } from 'react'
import { Icon } from '../../../components/ui/Icon'
import { formatDate } from '../../../lib/formatDate'
import { formatFileSize } from '../../../lib/formatFileSize'
import { useAuth } from '../../auth/useAuth'
import { downloadDocument } from '../api/taskDetail.api'
import { useDeleteDocument } from '../hooks/useDeleteDocument'
import type { TaskDocument } from '../../../types/document.types'
import styles from './DocumentItem.module.css'

interface DocumentItemProps {
  taskId: number
  document: TaskDocument
}

/** Downloading goes through the authenticated axios client and a throwaway Blob URL, not a plain link —
 *  this app's auth token lives in a header, which a bare `<a href>` never sends. */
export function DocumentItem({ taskId, document }: DocumentItemProps) {
  const { currentUser, isDirector } = useAuth()
  const [downloading, setDownloading] = useState(false)
  const deleteDocument = useDeleteDocument(taskId)

  const canRemove = currentUser?.id === document.uploadedById || isDirector

  async function handleDownload() {
    setDownloading(true)
    try {
      await downloadDocument(taskId, document.id, document.fileName)
    } finally {
      setDownloading(false)
    }
  }

  return (
    <div className={styles.row}>
      <Icon name="file" size={18} className={styles.fileIcon} />
      <div className={styles.info}>
        <span className={styles.fileName}>{document.fileName}</span>
        <span className={styles.meta}>
          {document.uploadedByName} · {formatDate(document.uploadedAt)} · {formatFileSize(document.fileSize)}
        </span>
      </div>
      <button type="button" className={styles.action} onClick={handleDownload} disabled={downloading}>
        {downloading ? 'Downloading…' : 'Download'}
      </button>
      {canRemove && (
        <button
          type="button"
          className={styles.removeAction}
          onClick={() => deleteDocument.mutate(document.id)}
          disabled={deleteDocument.isPending}
        >
          {deleteDocument.isPending ? 'Removing…' : 'Remove'}
        </button>
      )}
    </div>
  )
}
