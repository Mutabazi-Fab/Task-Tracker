import { useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { Modal } from '../../../components/ui/Modal'
import { ErrorMessage } from '../../../components/ui/ErrorMessage'
import { useCreateTask } from '../hooks/useCreateTask'
import { createSubtask } from '../api/tasks.api'
import { addDocument } from '../../taskDetail/api/taskDetail.api'
import { CreateTaskForm } from './CreateTaskForm'
import type { InlineSubtaskRow } from './InlineSubtasksField'

interface CreateTaskModalProps {
  open: boolean
  onClose: () => void
}

/** Form shell + submit — owns the mutation(s), CreateTaskForm owns only the fields. When
 *  the new task is Team-assigned with InlineSubtasksField rows, each row's createSubtask
 *  call needs the parent task's id, so it only fires after the parent's own mutation resolves. */
export function CreateTaskModal({ open, onClose }: CreateTaskModalProps) {
  const createTask = useCreateTask()
  const queryClient = useQueryClient()
  const [isCreatingSubtasks, setIsCreatingSubtasks] = useState(false)
  const [subtaskError, setSubtaskError] = useState<string | null>(null)
  const [isUploadingDocuments, setIsUploadingDocuments] = useState(false)
  const [documentError, setDocumentError] = useState<string | null>(null)

  async function handleSubmit(
    payload: Parameters<typeof createTask.mutateAsync>[0],
    subtasks: InlineSubtaskRow[],
    documents: File[],
  ) {
    setSubtaskError(null)
    setDocumentError(null)
    const created = await createTask.mutateAsync(payload)
    let hadFailure = false

    if (subtasks.length > 0) {
      setIsCreatingSubtasks(true)
      try {
        // Sequential, not Promise.all — task codes are MAX(sequence)+1 with no locking, so
        // concurrent requests could read the same max and collide on the unique constraint.
        for (const row of subtasks) {
          await createSubtask(created.id, {
            title: row.title.trim(),
            createdById: payload.createdById,
            assignedPersonId: Number(row.personId),
            dateAssigned: payload.dateAssigned,
            deadline: payload.deadline,
            openingNote: 'Added when the team was assigned.',
          })
        }
      } catch (err) {
        // The team task itself is already created and safe — only the inline subtasks
        // failed. Leaving the modal open surfaces what went wrong.
        const message = err && typeof err === 'object' && 'message' in err ? String(err.message) : null
        setSubtaskError(
          message
            ? `The task was created, but adding its subtasks failed: ${message}`
            : 'The task was created, but adding its subtasks failed.',
        )
        hadFailure = true
      } finally {
        setIsCreatingSubtasks(false)
      }
    }

    if (documents.length > 0) {
      setIsUploadingDocuments(true)
      try {
        // Sequential for the same reason as subtasks above, and to avoid saturating the pool with several large uploads at once.
        for (const file of documents) {
          await addDocument(created.id, file)
        }
      } catch (err) {
        const message = err && typeof err === 'object' && 'message' in err ? String(err.message) : null
        setDocumentError(
          message
            ? `The task was created, but uploading its documents failed: ${message}`
            : 'The task was created, but uploading its documents failed.',
        )
        hadFailure = true
      } finally {
        setIsUploadingDocuments(false)
      }
    }

    // createTask's own onSuccess already invalidated the task/dashboard/people/teams
    // queries — this covers the subtasks' rollup and documents list too.
    queryClient.invalidateQueries({ queryKey: ['tasks'] })
    // Only auto-close on a clean run — the task is already safely created either way, but
    // a partial failure should stay visible rather than silently vanish.
    if (!hadFailure) {
      onClose()
    }
  }

  return (
    <Modal open={open} onClose={onClose} title="New task">
      {createTask.isError && <ErrorMessage message={createTask.error.message} />}
      {subtaskError && <ErrorMessage message={subtaskError} />}
      {documentError && <ErrorMessage message={documentError} />}
      <CreateTaskForm
        onSubmit={handleSubmit}
        onCancel={onClose}
        submitting={createTask.isPending || isCreatingSubtasks || isUploadingDocuments}
      />
    </Modal>
  )
}
