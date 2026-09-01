import { Button } from '../../../components/ui/Button'
import { ErrorMessage } from '../../../components/ui/ErrorMessage'
import { Modal } from '../../../components/ui/Modal'
import { useDeleteTask } from '../hooks/useDeleteTask'
import styles from '../../tasks/components/CreateTaskForm.module.css'

interface DeleteTaskModalProps {
  taskId: number
  taskCode: string
  hasSubtasks: boolean
  open: boolean
  onClose: () => void
  onDeleted: () => void
}

/** No reason field — the backend doesn't take one for this endpoint. Deleting a top-level
 *  task takes its subtasks with it (cascade), so that's called out explicitly when there
 *  are any, rather than being a silent side effect. */
export function DeleteTaskModal({ taskId, taskCode, hasSubtasks, open, onClose, onDeleted }: DeleteTaskModalProps) {
  const deleteTask = useDeleteTask()

  function handleConfirm() {
    deleteTask.mutate(taskId, { onSuccess: onDeleted })
  }

  return (
    <Modal open={open} onClose={onClose} title={`Delete ${taskCode}?`}>
      <p>
        This permanently deletes {taskCode} and its full comment/reassignment history.
        {hasSubtasks && ' Its subtasks will be deleted along with it.'} This can't be undone.
      </p>

      {deleteTask.isError && <ErrorMessage message={deleteTask.error.message} />}

      <div className={styles.actions}>
        <Button type="button" variant="ghost" onClick={onClose} disabled={deleteTask.isPending}>
          Cancel
        </Button>
        <Button type="button" variant="danger" onClick={handleConfirm} disabled={deleteTask.isPending}>
          {deleteTask.isPending ? 'Deleting…' : 'Delete task'}
        </Button>
      </div>
    </Modal>
  )
}
