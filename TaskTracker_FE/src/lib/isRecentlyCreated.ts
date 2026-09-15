// How long a task counts as "new" for the badge on its list row/card. 24 hours rather than
// something shorter (5 minutes was floated, but discarded) — this is a task tracker people
// check periodically through their day, not a live chat; a 5-minute window would almost
// always have already closed by the time anyone but the creator looks at the list. A full
// day gives everyone a real chance to notice a freshly assigned task stands out, while
// still meaning something (not "new" for a week).
const NEW_TASK_WINDOW_MS = 24 * 60 * 60 * 1000

/** No backend flag to keep in sync — just compares createdAt against "now" at render time,
 *  so the badge quietly stops appearing on its own once the window passes, without needing
 *  a fresh fetch to notice. */
export function isRecentlyCreated(createdAt: string): boolean {
  return Date.now() - new Date(createdAt).getTime() < NEW_TASK_WINDOW_MS
}
