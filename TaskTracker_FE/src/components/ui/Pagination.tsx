import { Icon } from './Icon'
import styles from './Pagination.module.css'

interface PaginationProps {
  /** 0-indexed, matching the backend's Pageable. */
  page: number
  totalPages: number
  onChange: (page: number) => void
}

const MAX_VISIBLE_PAGES = 5

/** A sliding window of up to MAX_VISIBLE_PAGES page numbers centered on the current page,
 *  clamped to stay inside [0, totalPages). Keeps the control usable at any scale — a lane
 *  with hundreds of tasks gets the same five-button width as one with three pages. */
function pageWindow(page: number, totalPages: number): number[] {
  const half = Math.floor(MAX_VISIBLE_PAGES / 2)
  let start = Math.max(0, page - half)
  const end = Math.min(totalPages - 1, start + MAX_VISIBLE_PAGES - 1)
  start = Math.max(0, end - MAX_VISIBLE_PAGES + 1)
  return Array.from({ length: end - start + 1 }, (_, i) => start + i)
}

/** Numbered pagination — Previous/Next arrows plus a window of page-number buttons, the
 *  current one highlighted. Nothing renders at all for a single page. */
export function Pagination({ page, totalPages, onChange }: PaginationProps) {
  if (totalPages <= 1) return null

  return (
    <nav className={styles.wrap} aria-label="Pagination">
      <button
        type="button"
        className={styles.arrow}
        onClick={() => onChange(page - 1)}
        disabled={page === 0}
        aria-label="Previous page"
      >
        <Icon name="chevronLeft" size={16} />
      </button>

      {pageWindow(page, totalPages).map((p) => (
        <button
          key={p}
          type="button"
          className={p === page ? styles.pageActive : styles.page}
          onClick={() => onChange(p)}
          aria-current={p === page ? 'page' : undefined}
        >
          {p + 1}
        </button>
      ))}

      <button
        type="button"
        className={styles.arrow}
        onClick={() => onChange(page + 1)}
        disabled={page + 1 >= totalPages}
        aria-label="Next page"
      >
        <Icon name="chevronRight" size={16} />
      </button>
    </nav>
  )
}
