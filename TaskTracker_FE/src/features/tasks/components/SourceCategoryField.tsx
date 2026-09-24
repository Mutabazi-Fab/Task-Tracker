import { useState } from 'react'
import { Button } from '../../../components/ui/Button'
import { SelectField } from '../../../components/ui/SelectField'
import { TextField } from '../../../components/ui/TextField'
import { useAuth } from '../../auth/useAuth'
import { useAddSourceCategory } from '../hooks/useAddSourceCategory'
import { useSourceCategories } from '../hooks/useSourceCategories'
import styles from './SourceCategoryField.module.css'

interface SourceCategoryFieldProps {
  value: string
  onChange: (value: string) => void
}

/** The Source dropdown itself — its options come from the saved, open TaskSourceCategory list rather
 *  than a fixed set, so an Executive/Super Admin can add a brand new category (e.g. a source Zigama
 *  hasn't needed before) via the "+" button next to it. */
export function SourceCategoryField({ value, onChange }: SourceCategoryFieldProps) {
  const { currentUser, isExecutive } = useAuth()
  const categoriesQuery = useSourceCategories()
  const addCategory = useAddSourceCategory()
  const [adding, setAdding] = useState(false)
  const [newName, setNewName] = useState('')

  const categories = categoriesQuery.data ?? []

  function handleAdd() {
    const trimmed = newName.trim()
    if (trimmed === '' || !currentUser) return
    addCategory.mutate(
      { name: trimmed, addedById: currentUser.id },
      {
        onSuccess: (category) => {
          onChange(category.name)
          setNewName('')
          setAdding(false)
        },
      },
    )
  }

  return (
    <div className={styles.wrap}>
      <div className={styles.row}>
        <div className={styles.field}>
          <SelectField
            label="Source (optional)"
            value={value}
            onChange={onChange}
            placeholder={categoriesQuery.isLoading ? 'Loading…' : 'Where this came from'}
            options={categories.map((c) => ({ label: c.name, value: c.name }))}
          />
        </div>
        {isExecutive && !adding && (
          <button type="button" className={styles.addButton} onClick={() => setAdding(true)} title="Add a new source">
            + Add
          </button>
        )}
      </div>

      {adding && (
        <div className={styles.row}>
          <div className={styles.field}>
            <TextField label="New source name" value={newName} onChange={setNewName} placeholder="e.g. Partner Bank" />
          </div>
          <Button type="button" variant="secondary" onClick={handleAdd} disabled={newName.trim() === '' || addCategory.isPending}>
            {addCategory.isPending ? 'Saving…' : 'Save'}
          </Button>
          <Button
            type="button"
            variant="ghost"
            onClick={() => {
              setAdding(false)
              setNewName('')
            }}
          >
            Cancel
          </Button>
        </div>
      )}
    </div>
  )
}
