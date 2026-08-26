# Throughline — Frontend

A military-styled task-progress tracking UI for the Throughline API. React 18 +
Vite + TypeScript, React Router v6, TanStack Query for all server state,
Axios, Recharts, plain CSS Modules — no Tailwind, no component library.

## Running it

```bash
npm install
npm run dev
```

The app expects the Spring Boot backend at `http://localhost:8080` (see
`src/api/axiosClient.ts`) — start `TaskTracker_BE` first, or every page will
show its error state instead of data.

## How this is organized

Organized **by feature, not by file type**. If you're changing something on
the dashboard, everything for the dashboard lives in `src/features/dashboard/`
— you shouldn't need to leave that folder. `src/components/`, `src/hooks/`,
and `src/lib/` are the exception: shared, feature-agnostic pieces reused
across more than one feature.

```
src/
├── main.tsx                 Entry point. Mounts <App/> — nothing else to edit here.
├── App.tsx                  Router + QueryClientProvider + ThemeProvider + AppShell wiring only.
│                             No feature logic — if you're adding a page, it doesn't belong here.
│
├── app/
│   ├── routes.tsx           Every route path (ROUTES) AND the <Route> tree (AppRoutes) — the
│   │                         one place to add a new page or change a URL.
│   ├── queryClient.ts        React Query defaults (staleTime, retries) + the ApiError type
│   │                         registration. Touch this to change caching behaviour app-wide.
│   └── NotFoundPage.tsx      The catch-all "*" route. Edit only if you want the 404 UI to change.
│
├── api/
│   ├── axiosClient.ts        Base URL + the response interceptor that normalizes backend errors
│   │                         into ApiError. Change the backend URL here.
│   └── endpoints.ts          Every backend URL as one constant/builder. Add new backend routes
│                              here first — nowhere else should hardcode a "/api/v1/..." string.
│
├── types/                    Shared TypeScript types, mirrored from the backend's DTOs — one file
│                              per domain (task, comment, reassignment, person, team, dashboard).
│                              Edit when a backend response shape changes.
│
├── styles/
│   ├── tokens.css             ALL design tokens (colours, spacing, radii) as CSS variables, for
│   │                          both the "field" and "command" themes. Change the look here, never
│   │                          with a raw hex inside a component.
│   ├── reset.css              Box-sizing/margin/font-smoothing reset. Rarely touched.
│   └── typography.css         Shared heading/body/mono utility classes.
│
├── components/                Shared, dumb, reusable — no feature-specific data-fetching.
│   ├── layout/                AppShell (sidebar/mobile-tab-bar swap), Sidebar, PageHeader, etc.
│   │                          Edit here for anything about the app's overall chrome/navigation.
│   ├── ui/                    Card, Button, StatCard, ProgressBar, StatusChip, Icon, form fields,
│   │                          Modal, EmptyState, LoadingSpinner, ErrorMessage. Edit here to change
│   │                          how a primitive looks everywhere at once.
│   └── feedback/              QueryBoundary — the one place isLoading/isError is handled for any
│                               query, so no page hand-rolls its own loading/error branches.
│
├── hooks/                     App-wide hooks with no feature ownership: useMediaQuery, useDebounce.
│
├── lib/                       Pure functions, zero React: formatDate, formatPercentage, getInitials,
│                              deriveStatus, statusColor. Add a new one here only if more than one
│                              feature would use it — otherwise it belongs in that feature.
│
└── features/
    ├── dashboard/              Org-wide reporting: KPIs, the progress-over-time trend, the status
    │                          donut, team leaderboard, people summary. Edit hooks/api here to
    │                          change what the dashboard fetches; components/ for how it's shown.
    ├── tasks/                  The task list: table/lanes toggle, status filter, search, and
    │                          create-task. Edit here for anything about browsing or creating tasks.
    ├── taskDetail/              One task's page: progress panel, comment timeline (the only way
    │                          progress changes), reassignment history/modal. Edit here for
    │                          anything about a single task's detail view.
    ├── people/                  People list + person profile: statistics, the plain-language
    │                          narrative, task-history-with-involvement-label. Read-only by design
    │                          — no create/edit/delete UI, matching what was asked for.
    ├── teams/                   Teams list + team page: stats, member chips, team's task list.
    │                          Also read-only by design.
    ├── search/                  The global search box (lives in the app header) and its results
    │                          page. Edit here for anything about cross-entity search.
    └── theme/                   ThemeProvider/useTheme/ThemeToggle — Field (light) vs Command
                                (dark), persisted to localStorage.
```

Within a feature, the sub-structure is always the same:
- `api/*.api.ts` — raw axios calls, typed, no React.
- `hooks/*.ts` — TanStack Query hooks wrapping those calls. Mutations invalidate every query
  key their change could affect (a comment can move dashboard/person/team numbers, not just
  the one task).
- `components/*.tsx` + co-located `*.module.css` — one thing per file, named after what it renders.
- The page component at the feature's root composes those pieces; it holds no fetching or
  layout logic of its own.

## Known deviations from a literal 1:1 build, and why

- **`features/teams/hooks/useTeamMembers.ts`** isn't in the original hook list. Neither
  `TeamResponse` nor `TeamStatisticsResponse` carries member ids (only `memberCount` /
  `memberProgresses[].name`), so `TeamMemberChip`'s "links to their profile" requirement was
  otherwise unmeetable — this hook fetches all people and filters by `teamId` client-side.
- **`TaskResultsSection` (search) and `TeamTaskList` (teams)** both reuse
  `features/tasks/components/TaskTable` instead of duplicating a second table/mobile-row
  renderer — it's a pure `TaskListItem[] → JSX` component with no dependency on tasks-feature
  state, so reuse costs nothing.
- **`AppShell` imports `SearchInput`** from `features/search`, the one place a layout
  component reaches into a feature. A global search box has to appear on every route by
  construction, so its usage site can't be feature-scoped even though its implementation
  still lives entirely inside `features/search/`.
- **Dashboard leaderboard/summary rows aren't clickable.** `TeamLeaderboardResponse` and
  `PersonSummaryResponse` carry names only, no ids — not a frontend gap, just what those two
  backend endpoints return.
