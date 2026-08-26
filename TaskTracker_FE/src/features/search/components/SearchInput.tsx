import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { ROUTES } from '../../../app/routes'
import { Icon } from '../../../components/ui/Icon'
import styles from './SearchInput.module.css'

/** Lives in the app header — submits to /search?q=, not a live-filtering box. */
export function SearchInput() {
  const [value, setValue] = useState('')
  const navigate = useNavigate()

  function handleSubmit(e: FormEvent) {
    e.preventDefault()
    const q = value.trim()
    if (q.length === 0) return
    navigate(`${ROUTES.search}?q=${encodeURIComponent(q)}`)
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit} role="search">
      <Icon name="search" size={15} />
      <input
        className={styles.input}
        value={value}
        onChange={(e) => setValue(e.target.value)}
        placeholder="Search people or tasks…"
        aria-label="Global search"
      />
    </form>
  )
}
