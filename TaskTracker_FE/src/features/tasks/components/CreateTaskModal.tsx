import { useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { Modal } from '../../../components/ui/Modal'
import { ErrorMessage } from '../../../components/ui/ErrorMessage'
import { useCreateTask } from '../hooks/useCreateTask'
import { createSubtask } from '../api/tasks.api'
import { CreateTaskForm } from './CreateTaskForm'
import type { InlineSubtaskRow } from './InlineSubtasksField'

interface CreateTaskModalProps {
  open: boolean
  onClose: () => void
}

/** Form shell + submit — owns the mutation(s), CreateTaskForm owns only the fields.
 *
 *  When the new task is Team-assigned and InlineSubtasksField collected any rows, this is
 *  what actually turns those into real subtasks: the team task has to exist first (a
 *  subtask needs its parent's id), so each row's createSubtask call only fires after the
 *  parent's own mutation resolves — sequenced here, not something CreateTaskForm could do
 *  on its own since it never touches the API directly. */
export function CreateTaskModal({ open, onClose }: CreateTaskModalProps) {
  const createTask = useCreateTask()
  const queryClient = useQueryClient()
  const [isCreatingSubtasks, setIsCreatingSubtasks] = useState(false)
  const [subtaskError, setSubtaskError] = useState<string | null>(null)

  async function handleSubmit(payload: Parameters<typeof createTask.mutateAsync>[0], subtasks: InlineSubtaskRow[]) {
    setSubtaskError(null)
    const created = await createTask.mutateAsync(payload)

    if (subtasks.length === 0) {
      onClose()
      return
    }

    setIsCreatingSubtasks(true)
    try {
      // Sequential, not Promise.all — task codes are assigned as MAX(sequence)+1 at request
      // time with no locking, so firing these concurrently lets two requests read the same
      // max and collide on the unique task_code constraint. Awaiting one at a time means
      // each row's insert has already committed before the next one reads the max.
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
      // createTask's own onSuccess already invalidated the task list/dashboard/people/teams
      // queries for the parent — this covers the subtasks' own rollup effect on top of that.
      queryClient.invalidateQueries({ queryKey: ['tasks'] })
      onClose()
    } catch (err) {
      // The team task itself is already created and safe — only the inline subtasks
      // failed. Leaving the modal open (rather than silently losing this) means whoever's
      // filling it in can see what went wrong; the team task can still be found afterward
      // and given subtasks the normal way even if this is abandoned.
      const message = err && typeof err === 'object' && 'message' in err ? String(err.message) : null
      setSubtaskError(
        message
          ? `The task was created, but adding its subtasks failed: ${message}`
          : 'The task was created, but adding its subtasks failed.',
      )
    } finally {
      setIsCreatingSubtasks(false)
    }
  }

  return (
    <Modal open={open} onClose={onClose} title="New task">
      {createTask.isError && <ErrorMessage message={createTask.error.message} />}
      {subtaskError && <ErrorMessage message={subtaskError} />}
      <CreateTaskForm
        onSubmit={handleSubmit}
        onCancel={onClose}
        submitting={createTask.isPending || isCreatingSubtasks}
      />
    </Modal>
  )
}
