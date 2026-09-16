import { useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { Modal } from '../../../components/ui/Modal'
import { ErrorMessage } from '../../../components/ui/ErrorMessage'
import { useCreateSubtask } from '../hooks/useCreateSubtask'
import { createSubtask } from '../../tasks/api/tasks.api'
import { CreateSubtaskForm } from './CreateSubtaskForm'
import type { InlineSubtaskRow } from '../../tasks/components/InlineSubtasksField'
import type { TaskSource } from '../../../types/task.types'

interface CreateSubtaskModalProps {
  parentTaskId: number
  /** The parent's own team — meaningless (and unused) when isDepartmentImplementation is
   *  true, since that case picks a team org-wide instead of using one fixed team's roster. */
  teamId: number
  /** True only when the parent is a Department-assigned Executive task. */
  isDepartmentImplementation?: boolean
  /** The parent's own Department id — only meaningful (and only passed) alongside
   *  isDepartmentImplementation, so the team picker can be scoped to that Department's own
   *  teams instead of every team org-wide. */
  departmentId?: number
  /** The parent Department task's own source/sourceLabel — passed through to
   *  CreateSubtaskForm so the implementation task can inherit and lock it. See that form's
   *  own doc comment for the full reasoning. */
  parentSource?: TaskSource | null
  parentSourceLabel?: string | null
  open: boolean
  onClose: () => void
}

/** Form shell + submit — owns the mutation(s), CreateSubtaskForm owns only the fields.
 *
 *  When this is a Team-assigned implementation task and InlineSubtasksField collected any
 *  rows, this is what turns those into real depth-2 leaf subtasks: they need the
 *  implementation task's own id (not parentTaskId, which is one level higher — the CEO's
 *  Department task), so each row's createSubtask call only fires once THIS task's own
 *  mutation resolves. */
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

  async function handleSubmit(
    payload: Parameters<typeof createImplementationTask.mutateAsync>[0],
    subtasks: InlineSubtaskRow[],
  ) {
    setLeafSubtaskError(null)
    const created = await createImplementationTask.mutateAsync(payload)

    if (subtasks.length === 0) {
      onClose()
      return
    }

    setIsCreatingLeafSubtasks(true)
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
      // createImplementationTask's own onSuccess already invalidated the task list/
      // dashboard/people/teams queries for it — this covers the leaf subtasks' own rollup
      // effect on top of that.
      queryClient.invalidateQueries({ queryKey: ['tasks'] })
      onClose()
    } catch (err) {
      // The implementation task itself is already created and safe — only the inline leaf
      // subtasks failed. Leaving the modal open means whoever's filling it in can see what
      // went wrong; that task can still be found afterward and given subtasks the normal
      // way even if this is abandoned.
      const message = err && typeof err === 'object' && 'message' in err ? String(err.message) : null
      setLeafSubtaskError(
        message
          ? `The implementation task was created, but adding its subtasks failed: ${message}`
          : 'The implementation task was created, but adding its subtasks failed.',
      )
    } finally {
      setIsCreatingLeafSubtasks(false)
    }
  }

  return (
    <Modal open={open} onClose={onClose} title={isDepartmentImplementation ? 'New implementation task' : 'New subtask'}>
      {createImplementationTask.isError && <ErrorMessage message={createImplementationTask.error.message} />}
      {leafSubtaskError && <ErrorMessage message={leafSubtaskError} />}
      <CreateSubtaskForm
        teamId={teamId}
        isDepartmentImplementation={isDepartmentImplementation}
        departmentId={departmentId}
        parentSource={parentSource}
        parentSourceLabel={parentSourceLabel}
        onSubmit={handleSubmit}
        onCancel={onClose}
        submitting={createImplementationTask.isPending || isCreatingLeafSubtasks}
      />
    </Modal>
  )
}
