import { useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { ErrorMessage } from '../../../components/ui/ErrorMessage'
import { SelectField } from '../../../components/ui/SelectField'
import { TextField } from '../../../components/ui/TextField'
import { useAuth } from '../../auth/useAuth'
import { usePeople } from '../../people/hooks/usePeople'
import { useChangeDepartmentHead } from '../hooks/useChangeDepartmentHead'
import { useRenameDepartment } from '../hooks/useRenameDepartment'
import type { Department } from '../../../types/department.types'
import styles from './DepartmentAdminControls.module.css'

/** Super-Admin-only (see DepartmentPage) — rename and head-reassignment each go through
 *  their own endpoint, same split as TeamServiceImpl's rename vs. setTeamLeader. Deleting
 *  the department lives in the page header instead (see DepartmentPage/
 *  DeleteDepartmentModal), not here — that action is a wider Executive-or-above tier,
 *  broader than the Super-Admin-only controls this component owns. */
export function DepartmentAdminControls({ department }: { department: Department }) {
  const { currentUser } = useAuth()
  const peopleQuery = usePeople()
  const renameDepartment = useRenameDepartment(department.id)
  const changeHead = useChangeDepartmentHead(department.id)

  const [name, setName] = useState(department.name)
  const [headId, setHeadId] = useState(department.headDirectorId ? String(department.headDirectorId) : '')

  if (!currentUser) return null

  const directors = (peopleQuery.data ?? []).filter(
    (person) => person.role === 'DIRECTOR' || person.role === 'EXECUTIVE' || person.role === 'SUPER_ADMIN',
  )

  const canRename = name.trim() !== '' && name.trim() !== department.name
  const canChangeHead = headId !== '' && Number(headId) !== department.headDirectorId

  function handleRename(e: React.FormEvent) {
    e.preventDefault()
    if (!currentUser || !canRename) return
    renameDepartment.mutate({ name: name.trim(), changedById: currentUser.id })
  }

  function handleChangeHead(e: React.FormEvent) {
    e.preventDefault()
    if (!currentUser || !canChangeHead) return
    changeHead.mutate({ newHeadDirectorId: Number(headId), changedById: currentUser.id })
  }

  return (
    <Card>
      <div className={styles.wrap}>
        <span className={styles.heading}>Admin controls</span>

        <div className={styles.section}>
          <span className={styles.sectionLabel}>Rename</span>
          <form className={styles.row} onSubmit={handleRename}>
            <TextField label="Department name" value={name} onChange={setName} required />
            <Button type="submit" variant="secondary" disabled={!canRename || renameDepartment.isPending}>
              {renameDepartment.isPending ? 'Saving…' : 'Rename'}
            </Button>
          </form>
          {renameDepartment.isError && <ErrorMessage message={renameDepartment.error.message} />}
        </div>

        <div className={styles.section}>
          <span className={styles.sectionLabel}>Head Director</span>
          <form className={styles.row} onSubmit={handleChangeHead}>
            <div className={styles.field}>
              <SelectField
                label="Head Director"
                value={headId}
                onChange={setHeadId}
                placeholder="Select a head"
                options={directors.map((person) => ({ label: person.fullName, value: String(person.id) }))}
              />
            </div>
            <Button type="submit" variant="secondary" disabled={!canChangeHead || changeHead.isPending}>
              {changeHead.isPending ? 'Saving…' : 'Change head'}
            </Button>
          </form>
          {changeHead.isError && <ErrorMessage message={changeHead.error.message} />}
        </div>
      </div>
    </Card>
  )
}
