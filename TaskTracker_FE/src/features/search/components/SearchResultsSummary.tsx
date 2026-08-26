import styles from './SearchResultsSummary.module.css'

interface SearchResultsSummaryProps {
  query: string
  peopleCount: number
  taskCount: number
}

/** "3 people · 5 tasks matching …" */
export function SearchResultsSummary({ query, peopleCount, taskCount }: SearchResultsSummaryProps) {
  return (
    <p className={styles.summary}>
      {peopleCount} {peopleCount === 1 ? 'person' : 'people'} · {taskCount} {taskCount === 1 ? 'task' : 'tasks'} matching “
      {query}”
    </p>
  )
}
