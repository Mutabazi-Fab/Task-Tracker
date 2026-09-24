import { Button } from '../../../components/ui/Button'
import { ErrorMessage } from '../../../components/ui/ErrorMessage'
import { Modal } from '../../../components/ui/Modal'
import { useAuth } from '../../auth/useAuth'
import { useSetTeamLeader } from '../hooks/useSetTeamLeader'
import styles from './CreateTeamForm.module.css'

interface MakeLeaderModalProps {
  teamId: number
  teamName: string
  personId: number
  personName: string
  open: boolean
  onClose: () => void
}

/** A plain yes/no confirm, not a form — no reason is collected here, since the backend already knows
 *  exactly what changed (who the leader was, who it's becoming) and logs that itself. */
export function MakeLeaderModal({ teamId, teamName, personId, personName, open, onClose }: MakeLeaderModalProps) {
  const { currentUser } = useAuth()
  const setLeader = useSetTeamLeader(teamId)

  function handleConfirm() {
    if (!currentUser) return
    setLeader.mutate({ personId, changedById: currentUser.id }, { onSuccess: onClose })
  }

  return (
    <Modal open={open} onClose={onClose} title="Change team leader">
      <p>
        Make <strong>{personName}</strong> the new leader of <strong>{teamName}</strong>?
      </p>

      {setLeader.isError && <ErrorMessage message={setLeader.error.message} />}

      <div className={styles.actions}>
        <Button type="button" variant="ghost" onClick={onClose} disabled={setLeader.isPending}>
          No
        </Button>
        <Button type="button" variant="primary" onClick={handleConfirm} disabled={setLeader.isPending}>
          {setLeader.isPending ? 'Updating…' : 'Yes, make leader'}
        </Button>
      </div>
    </Modal>
  )
}
