import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ROUTES } from '../../../app/routePaths'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { ErrorMessage } from '../../../components/ui/ErrorMessage'
import { SelectField } from '../../../components/ui/SelectField'
import { formatPercentage } from '../../../lib/formatPercentage'
import { useAddDailyGoal } from '../../people/hooks/useAddDailyGoal'
import { useRemoveDailyGoal } from '../../people/hooks/useRemoveDailyGoal'
import type { TaskListItem } from '../../../types/task.types'
import styles from './DailyGoalCard.module.css'

const MAX_GOALS = 3

interface DailyGoalCardProps {
  personId: number
  dailyGoalTasks: TaskListItem[]
  myTasks: TaskListItem[]
}

interface PickableTask {
  id: number
  taskCode: string
  title: string
  /** The implementation/parent task this lives under, for display only — null for a real
   *  top-level task. */
  parentTitle: string | null
}

/** "What I'm focused on today" — up to 3 of a Member's own assigned tasks, picked by them, shown at the
 *  top of their dashboard. */
export function DailyGoalCard({ personId, dailyGoalTasks, myTasks }: DailyGoalCardProps) {
  const [pickerTaskId, setPickerTaskId] = useState('')
  const addGoal = useAddDailyGoal(personId)
  const removeGoal = useRemoveDailyGoal(personId)

  const goalTaskIds = new Set(dailyGoalTasks.map((t) => t.id))

  // The backend only accepts a task actually assigned to this person directly
  // (PersonServiceImpl.addDailyGoal) — myTasks also includes team-assigned tasks the person merely has
  // visibility into, which would fail that check silently.
  const ownTopLevelTasks: PickableTask[] = myTasks
    .filter((t) => t.assigneeType === 'INDIVIDUAL')
    .map((t) => ({ id: t.id, taskCode: t.taskCode, title: t.title, parentTitle: null }))
  const ownSubtasks: PickableTask[] = myTasks.flatMap((t) =>
    t.subtasks
      .filter((s) => s.assigneeType === 'INDIVIDUAL' && s.assigneeId === personId)
      .map((s) => ({ id: s.id, taskCode: s.taskCode, title: s.title, parentTitle: t.title })),
  )
  const pickableTasks = [...ownTopLevelTasks, ...ownSubtasks].filter((t) => !goalTaskIds.has(t.id))
  const atLimit = dailyGoalTasks.length >= MAX_GOALS

  function handleAdd() {
    if (pickerTaskId === '') return
    addGoal.mutate(Number(pickerTaskId), { onSuccess: () => setPickerTaskId('') })
  }

  return (
    <Card>
      <div className={styles.heading}>Today's goals</div>

      {dailyGoalTasks.length === 0 ? (
        <p className={styles.empty}>Pick up to {MAX_GOALS} tasks you want to focus on today.</p>
      ) : (
        <div className={styles.panels}>
          {dailyGoalTasks.map((task) => (
            <div key={task.id} className={styles.panel}>
              <Link to={ROUTES.taskDetail(task.id)} className={styles.taskLink}>
                {task.taskCode} · {task.title}
              </Link>
              {task.parentTaskTitle && <span className={styles.parentHint}>under {task.parentTaskTitle}</span>}
              {task.status === 'COMPLETED' ? (
                <span className={styles.done}>✓ Finished</span>
              ) : (
                <div className={styles.bar}>
                  <div className={styles.track}>
                    <div className={styles.fill} style={{ width: `${task.progressPercentage}%` }} />
                  </div>
                  <span className={styles.percentage}>{formatPercentage(task.progressPercentage)}</span>
                </div>
              )}
              <Button
                type="button"
                variant="ghost"
                onClick={() => removeGoal.mutate(task.id)}
                disabled={removeGoal.isPending}
              >
                Remove
              </Button>
            </div>
          ))}
        </div>
      )}

      {removeGoal.isError && <ErrorMessage message={removeGoal.error.message} />}

      {atLimit ? (
        <p className={styles.limitNote}>You're focused on {MAX_GOALS} tasks — remove one to add another.</p>
      ) : (
        <div className={styles.picker}>
          <SelectField
            value={pickerTaskId}
            onChange={setPickerTaskId}
            placeholder={pickableTasks.length === 0 ? 'No individually-assigned tasks to pick from' : 'Pick one of your tasks or subtasks'}
            options={pickableTasks.map((t) => ({
              label: t.parentTitle ? `${t.taskCode} · ${t.title} (under ${t.parentTitle})` : `${t.taskCode} · ${t.title}`,
              value: String(t.id),
            }))}
          />
          <Button type="button" variant="secondary" onClick={handleAdd} disabled={pickerTaskId === '' || addGoal.isPending}>
            {addGoal.isPending ? 'Adding…' : "Add to today's goals"}
          </Button>
        </div>
      )}
      {addGoal.isError && <ErrorMessage message={addGoal.error.message} />}
    </Card>
  )
}
