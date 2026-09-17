import { Button } from '../../../components/ui/Button'
import { ErrorMessage } from '../../../components/ui/ErrorMessage'
import { Modal } from '../../../components/ui/Modal'
import { useDeleteDepartment } from '../hooks/useDeleteDepartment'
import styles from './DepartmentAdminControls.module.css'

interface DeleteDepartmentModalProps {
  departmentId: number
  departmentName: string
  open: boolean
  onClose: () => void
  onDeleted: () => void
}

/** Only reachable when the department is already empty — DepartmentServiceImpl.
 *  deleteDepartment refuses one that still has teams or people, so this confirmation is
 *  about the deletion itself, not about an unexpected cascade. */
export function DeleteDepartmentModal({
  departmentId,
  departmentName,
  open,
  onClose,
  onDeleted,
}: DeleteDepartmentModalProps) {
  const deleteDepartment = useDeleteDepartment()

  function handleConfirm() {
    deleteDepartment.mutate(departmentId, { onSuccess: onDeleted })
  }

  return (
    <Modal open={open} onClose={onClose} title={`Delete ${departmentName}?`}>
      <p>This permanently deletes {departmentName}. This can't be undone.</p>

      {deleteDepartment.isError && <ErrorMessage message={deleteDepartment.error.message} />}

      <div className={styles.actions}>
        <Button type="button" variant="ghost" onClick={onClose} disabled={deleteDepartment.isPending}>
          Cancel
        </Button>
        <Button type="button" variant="danger" onClick={handleConfirm} disabled={deleteDepartment.isPending}>
          {deleteDepartment.isPending ? 'Deleting…' : 'Delete department'}
        </Button>
      </div>
    </Modal>
  )
}
