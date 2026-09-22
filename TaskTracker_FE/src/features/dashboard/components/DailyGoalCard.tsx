import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ROUTES } from '../../../app/routePaths'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
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

/** "What I'm focused on today" — up to 3 of a Member's own assigned tasks, picked by them,
 *  shown at the top of their dashboard. Removing a goal is always a manual, explicit
 *  action — a completed task doesn't disappear on its own, it just switches to a "done"
 *  treatment until the Member clears it themselves. */
export function DailyGoalCard({ personId, dailyGoalTasks, myTasks }: DailyGoalCardProps) {
  const [pickerTaskId, setPickerTaskId] = useState('')
  const addGoal = useAddDailyGoal(personId)
  const removeGoal = useRemoveDailyGoal(personId)

  const goalTaskIds = new Set(dailyGoalTasks.map((t) => t.id))
  const pickableTasks = myTasks.filter((t) => !goalTaskIds.has(t.id))
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
        <div className={styles.slots}>
          {dailyGoalTasks.map((task) => (
            <div key={task.id} className={styles.slot}>
              <Link to={ROUTES.taskDetail(task.id)} className={styles.taskLink}>
                {task.taskCode} · {task.title}
              </Link>
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

      {atLimit ? (
        <p className={styles.limitNote}>You're focused on {MAX_GOALS} tasks — remove one to add another.</p>
      ) : (
        <div className={styles.picker}>
          <SelectField
            value={pickerTaskId}
            onChange={setPickerTaskId}
            placeholder="Pick one of your tasks"
            options={pickableTasks.map((t) => ({ label: `${t.taskCode} · ${t.title}`, value: String(t.id) }))}
          />
          <Button type="button" variant="secondary" onClick={handleAdd} disabled={pickerTaskId === '' || addGoal.isPending}>
            {addGoal.isPending ? 'Adding…' : "Add to today's goals"}
          </Button>
        </div>
      )}
    </Card>
  )
}
