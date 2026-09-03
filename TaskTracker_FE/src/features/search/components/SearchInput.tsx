import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
// From routePaths directly, not '../../../app/routes' — that file imports AppShell, which
// renders this component, which would make ROUTES a circular import (see routePaths.ts).
import { ROUTES } from '../../../app/routePaths'
import { Icon } from '../../../components/ui/Icon'
import { useGlobalSearch } from '../hooks/useGlobalSearch'
import styles from './SearchInput.module.css'

const MAX_PREVIEW_ITEMS = 5

/**
 * Lives in the app header. Typing narrows a live results dropdown right underneath the
 * box (people and tasks, same data as the full /search page, capped to a handful of each)
 * — pressing Enter, or "View all results", still goes to the full page for everything the
 * dropdown doesn't have room for. Same click-outside/dropdown-panel pattern as
 * NotificationBell, so the two headers controls behave consistently.
 */
export function SearchInput() {
  const [value, setValue] = useState('')
  const [open, setOpen] = useState(false)
  const wrapRef = useRef<HTMLDivElement>(null)
  const navigate = useNavigate()

  const { searchQuery: query, debouncedQuery } = useGlobalSearch(value)
  const trimmed = value.trim()

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (wrapRef.current && !wrapRef.current.contains(e.target as Node)) {
        setOpen(false)
      }
    }
    function handleEscape(e: KeyboardEvent) {
      if (e.key === 'Escape') setOpen(false)
    }
    if (open) {
      document.addEventListener('mousedown', handleClickOutside)
      document.addEventListener('keydown', handleEscape)
    }
    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
      document.removeEventListener('keydown', handleEscape)
    }
  }, [open])

  function goToFullResults() {
    if (trimmed.length === 0) return
    setOpen(false)
    navigate(`${ROUTES.search}?q=${encodeURIComponent(trimmed)}`)
  }

  function handleSubmit(e: FormEvent) {
    e.preventDefault()
    goToFullResults()
  }

  const people = query.data?.people ?? []
  const tasks = query.data?.tasks ?? []
  const hasMore = people.length > MAX_PREVIEW_ITEMS || tasks.length > MAX_PREVIEW_ITEMS
  const showDropdown = open && trimmed.length > 0
  // True while the debounce hasn't caught up to the latest keystroke yet, or the query is
  // genuinely mid-fetch with no data yet — either way, "no matches" would be wrong to show.
  const isSearchPending = debouncedQuery !== trimmed || (query.isFetching && !query.data)

  return (
    <div className={styles.wrap} ref={wrapRef}>
      <form className={styles.form} onSubmit={handleSubmit} role="search">
        <Icon name="search" size={15} />
        <input
          className={styles.input}
          value={value}
          onChange={(e) => {
            setValue(e.target.value)
            setOpen(true)
          }}
          onFocus={() => setOpen(true)}
          placeholder="Search people or tasks…"
          aria-label="Global search"
        />
      </form>

      {showDropdown && (
        <div className={styles.panel}>
          {isSearchPending ? (
            <div className={styles.empty}>Searching…</div>
          ) : query.isError ? (
            <div className={styles.empty}>{query.error.message}</div>
          ) : people.length === 0 && tasks.length === 0 ? (
            <div className={styles.empty}>No matches for "{trimmed}"</div>
          ) : (
            <>
              {people.length > 0 && (
                <div className={styles.section}>
                  <div className={styles.sectionHeading}>People</div>
                  {people.slice(0, MAX_PREVIEW_ITEMS).map(({ person }) => (
                    <Link
                      key={person.id}
                      to={ROUTES.personProfile(person.id)}
                      className={styles.item}
                      onClick={() => setOpen(false)}
                    >
                      <span className={styles.itemTitle} title={person.fullName}>
                        {person.fullName}
                      </span>
                      <span className={styles.itemMeta}>{person.jobTitle}</span>
                    </Link>
                  ))}
                </div>
              )}

              {tasks.length > 0 && (
                <div className={styles.section}>
                  <div className={styles.sectionHeading}>Tasks</div>
                  {tasks.slice(0, MAX_PREVIEW_ITEMS).map((task) => (
                    <Link
                      key={task.id}
                      to={ROUTES.taskDetail(task.id)}
                      className={styles.item}
                      onClick={() => setOpen(false)}
                    >
                      <span className={styles.itemTitle} title={task.title}>
                        {task.title}
                      </span>
                      <span className={styles.itemMeta}>{task.taskCode}</span>
                    </Link>
                  ))}
                </div>
              )}

              <button type="button" className={styles.viewAll} onClick={goToFullResults}>
                {hasMore ? `View all results for "${trimmed}"` : `View full results for "${trimmed}"`}
              </button>
            </>
          )}
        </div>
      )}
    </div>
  )
}
