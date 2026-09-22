import { useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { Modal } from '../../../components/ui/Modal'
import { ErrorMessage } from '../../../components/ui/ErrorMessage'
import { useCreateSubtask } from '../hooks/useCreateSubtask'
import { createSubtask } from '../../tasks/api/tasks.api'
import { addDocument } from '../api/taskDetail.api'
import { CreateSubtaskForm } from './CreateSubtaskForm'
import type { InlineSubtaskRow } from '../../tasks/components/InlineSubtasksField'
import type { TaskSource } from '../../../types/task.types'

interface CreateSubtaskModalProps {
  parentTaskId: number
  /** Unused when isDepartmentImplementation is true, which picks a team org-wide instead. */
  teamId: number
  /** True only when the parent is a Department-assigned Executive task. */
  isDepartmentImplementation?: boolean
  /** Only meaningful alongside isDepartmentImplementation — scopes the team picker to this Department's own teams. */
  departmentId?: number
  /** Passed through to CreateSubtaskForm so the implementation task can inherit and lock it. */
  parentSource?: TaskSource | null
  parentSourceLabel?: string | null
  open: boolean
  onClose: () => void
}

/** Form shell + submit — owns the mutation(s), CreateSubtaskForm owns only the fields.
 *  When this is a Team-assigned implementation task with InlineSubtasksField rows, each
 *  row's createSubtask call needs THIS task's own id (not parentTaskId, one level higher),
 *  so it only fires once this task's own mutation resolves. */
export function CreateSubtaskModal({
  parentTaskId,
  teamId,
  isDepartmentImplementation,
  departmentId,
  parentSource,
  parentSourceLabel,
  open,
  onClose,
}: CreateSubtaskModalProps) {
  const createImplementationTask = useCreateSubtask(parentTaskId)
  const queryClient = useQueryClient()
  const [isCreatingLeafSubtasks, setIsCreatingLeafSubtasks] = useState(false)
  const [leafSubtaskError, setLeafSubtaskError] = useState<string | null>(null)
  const [isUploadingDocuments, setIsUploadingDocuments] = useState(false)
  const [documentError, setDocumentError] = useState<string | null>(null)

  async function handleSubmit(
    payload: Parameters<typeof createImplementationTask.mutateAsync>[0],
    subtasks: InlineSubtaskRow[],
    documents: File[],
  ) {
    setLeafSubtaskError(null)
    setDocumentError(null)
    const created = await createImplementationTask.mutateAsync(payload)
    let hadFailure = false

    if (subtasks.length > 0) {
      setIsCreatingLeafSubtasks(true)
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
        // The implementation task itself is already created and safe — only the inline leaf
        // subtasks failed. Leaving the modal open surfaces what went wrong.
        const message = err && typeof err === 'object' && 'message' in err ? String(err.message) : null
        setLeafSubtaskError(
          message
            ? `The implementation task was created, but adding its subtasks failed: ${message}`
            : 'The implementation task was created, but adding its subtasks failed.',
        )
        hadFailure = true
      } finally {
        setIsCreatingLeafSubtasks(false)
      }
    }

    if (documents.length > 0) {
      setIsUploadingDocuments(true)
      try {
        // Sequential for the same reason as leaf subtasks above.
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

    // createImplementationTask's own onSuccess already invalidated the task/dashboard/
    // people/teams queries — this covers the leaf subtasks' rollup and documents list too.
    queryClient.invalidateQueries({ queryKey: ['tasks'] })
    if (!hadFailure) {
      onClose()
    }
  }

  return (
    <Modal open={open} onClose={onClose} title={isDepartmentImplementation ? 'New implementation task' : 'New subtask'}>
      {createImplementationTask.isError && <ErrorMessage message={createImplementationTask.error.message} />}
      {leafSubtaskError && <ErrorMessage message={leafSubtaskError} />}
      {documentError && <ErrorMessage message={documentError} />}
      <CreateSubtaskForm
        teamId={teamId}
        isDepartmentImplementation={isDepartmentImplementation}
        departmentId={departmentId}
        parentSource={parentSource}
        parentSourceLabel={parentSourceLabel}
        onSubmit={handleSubmit}
        onCancel={onClose}
        submitting={createImplementationTask.isPending || isCreatingLeafSubtasks || isUploadingDocuments}
      />
    </Modal>
  )
}
