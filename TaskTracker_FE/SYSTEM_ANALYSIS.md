# Comprehensive A–Z System Analysis: Throughline (Task Tracker)

---

## 1. Overall System Understanding

### 1.1 Core System Purpose & Design Philosophy
**Throughline** is an enterprise task progress tracking and accountability management platform. It is built around a foundational doctrine:
> **"Every percentage is justified by a dated comment, and every reassignment is justified by a reason."**

The application is themed around a Rwandan military and corporate banking administrative hierarchy—incorporating organizational structures such as **Departments**, **Directors**, **Executives**, **Teams**, **Team Leaders**, and **Military/Civilian Ranks** (e.g., Major, Captain, Lieutenant Colonel, alongside civilian job titles). Underneath this organizational metaphor lies a rigorous, audit-first workflow engine engineered to eliminate unverified status reporting, untracked reassignments, and opaque organizational changes.

### 1.2 System Goals & Invariants
1. **Unidirectional Justified Progress**: Task progress percentages are never directly modified via simple input sliders or raw number fields. A task's progress moves forward only through an append-only log of timestamped progress comments authored by responsible team members.
2. **Deterministic Status Derivation**: Task statuses (`PENDING`, `ONGOING`, `COMPLETED`) are strictly derived from the current progress percentage:
   $$\text{Status} = \begin{cases} \text{PENDING} & \text{if } \text{progress} = 0\% \\ \text{COMPLETED} & \text{if } \text{progress} = 100\% \\ \text{ONGOING} & \text{if } 0\% < \text{progress} < 100\% \end{cases}$$
   Status is never manually selected by any user.
3. **Hierarchical Rollup Calculations**: Tasks form a strict parent-child hierarchy. A top-level initiative assigned to an entire Team or Department cannot have its percentage modified directly by comments; its percentage is automatically calculated as the average of its child subtasks, bubbling upward recursively.
4. **Permanent Auditability**: Deletions and status transitions across people, roles, teams, and tasks are recorded in dedicated append-only log entities (`TaskActivity`, `RoleChange`, `AccountStatusChange`, `TeamMembershipChange`, `DepartmentActivity`, `TaskReassignment`).
5. **Role-Scoped Visibility**: Data visibility is strictly segregated at the data layer. Frontline members see only what directly pertains to them and their immediate teams, whereas Directors, Executives, and Super Admins oversee broader department and organization-wide scopes.

### 1.3 User Roles & Authority Matrix

```
[MEMBER] < [DIRECTOR] < [EXECUTIVE] < [SUPER_ADMIN]
              ^
        [TEAM LEADER] (Contextual, per-team role)
```

* **Member**:
  * Can view only tasks assigned directly to them or to teams they belong to.
  * On the People page, can view only colleagues who share at least one team with them.
  * For teams they are not on, can see only the team name and leader name.
  * Can log progress comments and discussion comments on accessible tasks.
  * Can request deadline extensions on their assigned tasks.
* **Team Leader** (Contextual per-team role, assigned to a Member via [TeamMember](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_BE/src/main/java/com/throughline/taskmanagement/model/TeamMember.java)):
  * Can break down their team's top-level tasks into leaf subtasks assigned to individual members of that same team.
  * Can reassign subtasks among team members (with mandatory reason).
  * Can add or remove members from their team (with mandatory reason).
* **Director**:
  * Organization-wide visibility over people, teams, and tasks (scoped to their department for operational tasks, org-wide for oversight).
  * Can create teams within the department they head.
  * Can create top-level team-assigned tasks.
  * Can onboard new people (defaulting to the Member role).
  * Can reassign leaders within teams belonging to their department.
  * Accesses the Director Dashboard ("My Initiatives" filter, KPIs, trends, leaderboards).
  * Can pin/unpin tasks within their department.
  * Can delete tasks they created within their department.
* **Executive**:
  * Sits at the executive/CEO tier.
  * Possesses all Director permissions across all departments.
  * Can create top-level tasks assigned directly to an entire Department (`AssigneeType.DEPARTMENT`).
  * Can classify task severity (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`).
  * Can create new departments and delete empty departments.
  * Accesses the Executive Dashboard (department health traffic-light matrix, executive KPIs, critical task oversight).
  * Decides on or approves forwarded deadline extension requests.
* **Super Admin**:
  * Highest governance authority.
  * Inherits all Executive and Director capabilities.
  * Exclusively owns system-level governance: promoting/demoting user roles (`MEMBER`, `DIRECTOR`, `EXECUTIVE`, `SUPER_ADMIN`), activating/deactivating accounts, renaming departments, and reassigning department heads.
  * Can trigger administrative password resets for locked-out accounts.
  * Accesses security audit feeds (Role Changes, Account Status Changes).

### 1.4 High-Level System Architecture & Component Interaction

```
+-----------------------------------------------------------------------+
|                         Vite + React 19 Frontend                      |
|  [React Router v6]  <--->  [TanStack Query Cache]  <--->  [Axios]    |
|   (AppShell, Pages)          (Server State, Polling)       (JWT Auth) |
+-----------------------------------------------------------------------+
                                  | HTTP / JSON
                                  v
+-----------------------------------------------------------------------+
|                       Spring Boot 4 Backend                           |
|  [Security Filter Chain] -> JwtAuthenticationFilter -> RateLimiter    |
|  [REST Controllers]      -> CurrentPersonResolver                     |
|  [Service Layer]         -> Business Invariants, Authz, Mail, Events  |
|  [Spring Data JPA]       -> Repositories, Custom JPQL, EntityGraphs   |
+-----------------------------------------------------------------------+
                                  | JDBC
                                  v
+-----------------------------------------------------------------------+
|                        PostgreSQL Database                            |
|  15 Relational Tables: persons, tasks, comments, teams, depts, etc.   |
+-----------------------------------------------------------------------+
```

Data flows strictly in a decoupled client-server model. The frontend runs as a Single Page Application (SPA) on port `5173`, communicating via Axios HTTP requests to the Spring Boot REST API on port `8080`. State is not stored in HTTP sessions; requests carry a signed JWT token in an `Authorization: Bearer <token>` header.

---

## 2. Frontend Analysis

### 2.1 Technology Stack & Structure
* **Core Libraries**: React 19.2.7, TypeScript 7.0.2, Vite 8.1.1.
* **Routing**: React Router DOM 6.30.6.
* **Server State & Caching**: TanStack React Query 5.102.5.
* **HTTP Client**: Axios 1.20.0 with request/response interceptors.
* **Visualizations**: Recharts 3.10.1.
* **Styling**: Vanilla CSS Modules (`*.module.css`) backed by an app-wide CSS token system (`tokens.css`, `reset.css`, `typography.css`). No CSS frameworks (Tailwind, Bootstrap) or third-party UI component libraries (MUI, AntD) are used.
* **Linter**: Oxlint 1.71.0.

#### Directory Layout
```
TaskTracker_FE/src/
├── api/             # Axios client, error interceptors, storage keys
├── app/             # QueryClient setup, route definitions, central route paths
├── components/
│   ├── feedback/    # QueryBoundary (unified loading/error wrapper)
│   ├── layout/      # AppShell, Sidebar, MobileTabBar, PageHeader
│   └── ui/          # Atomic components (Button, Modal, Card, Badge, etc.)
├── features/        # Feature-sliced domains:
│   ├── auth/        # Context, login, signup, OTP, password resets
│   ├── dashboard/   # Charts, leaderboards, KPIs, RequestsPage
│   ├── departments/ # Org chart, department detail, admin modals
│   ├── notifications/# NotificationBell dropdown, unread count hooks
│   ├── people/      # Directory, teammate grid, profile, role change modals
│   ├── search/      # Top-bar debounced search input, search results page
│   ├── taskDetail/  # Task overview, sparkline, subtasks, discussion, docs
│   ├── tasks/       # Task table, status chips, lanes board, activity log
│   ├── teams/       # Team cards, roster management, membership logs
│   └── theme/       # Dual theme provider (field vs. command mode)
├── hooks/           # useMediaQuery, useDebounce
├── icon/            # Custom SVG icon set
├── lib/             # Date formatters, recent item calculators
├── styles/          # CSS design tokens, typography, CSS resets
└── types/           # Strongly-typed TypeScript interfaces
```

### 2.2 Navigation, Routing & Layout Architecture
Navigation paths are centralized in [routePaths.ts](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_FE/src/app/routePaths.ts). This file has zero imports to prevent cyclic dependency initialization crashes between [routes.tsx](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_FE/src/app/routes.tsx), [AppShell.tsx](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_FE/src/components/layout/AppShell.tsx), and [Sidebar.tsx](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_FE/src/components/layout/Sidebar.tsx).

* **Public vs. Protected Routes**:
  * Public routes (`/login`, `/signup`, `/verify-email`, `/forgot-password`, `/reset-password`) are wrapped in `<PublicOnlyRoute>`. If a user with a valid session hits them, they are redirected immediately to `/`.
  * Authenticated routes are wrapped in `<ProtectedRoute>` and embedded inside `<AppShell>`.
* **Responsive Layout ([AppShell.tsx](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_FE/src/components/layout/AppShell.tsx))**:
  * Desktop ($\ge 768\text{px}$): Permanent left-hand sidebar with logo, navigation links, unread notification counter badges, user profile tile, logout button, and theme switcher.
  * Mobile ($< 768\text{px}$): Bottom tab bar (`MobileTabBar`) replaces the sidebar; the main layout collapses into a single column.
  * Persistent Top Bar: Houses the `NotificationBell` dropdown and the global `SearchInput`.

### 2.3 Frontend Authentication & Authorization Flow
1. **State Ownership ([AuthContext.tsx](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_FE/src/features/auth/AuthContext.tsx))**:
   * Stores `currentUser` (`Person | null`) and `status` (`loading | authenticated | unauthenticated`).
   * Provides booleans: `isDirector` (true for DIRECTOR, EXECUTIVE, SUPER_ADMIN), `isExecutive` (EXECUTIVE, SUPER_ADMIN), and `isSuperAdmin` (SUPER_ADMIN).
2. **Session Persistence**:
   * If "Remember Me" is checked during login, the JWT is stored in `localStorage`.
   * If unchecked, it is stored in `sessionStorage` (evaporates upon closing the browser tab).
3. **Session Hydration**:
   * On initial mount, if a token is present in storage, the frontend invokes `fetchCurrentPerson()` (`GET /api/v1/auth/me`).
   * If the fetch succeeds, `currentUser` is populated. If it fails or returns 401, token storage is cleared and the state transitions to `unauthenticated`.
4. **Decoupled 401 Handling**:
   * When any API request fails with a 401 status, [axiosClient.ts](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_FE/src/api/axiosClient.ts) dispatches a native `window.dispatchEvent(new CustomEvent('throughline:unauthorized'))`.
   * [AuthContext.tsx](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_FE/src/features/auth/AuthContext.tsx) listens to this event, clears storage, resets user state, and forces React Router to redirect to `/login`.

### 2.4 State Management Strategy
The application deliberately avoids external client-side state libraries (such as Redux, Zustand, or MobX):
* **Server State**: Managed 100% through TanStack Query (`useQuery`, `useMutation`, `useQueryClient`).
* **Cache Keys**: Structured hierarchically (e.g., `['tasks', { status, assignedPersonId, page }]`, `['task', taskId]`, `['notifications', personId]`).
* **Mutation & Optimistic Cache Invalidation**: On successful mutation (e.g., posting a comment, reassigning a task, uploading a document), queries are selectively invalidated via `queryClient.invalidateQueries({ queryKey: [...] })`, triggering background refetches.
* **UI/Local State**: Component-level `useState` handles modal visibility, active tabs, form inputs, and pagination indexes.

### 2.5 Reusable Feedback & UI Architecture
* **[QueryBoundary.tsx](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_FE/src/components/feedback/QueryBoundary.tsx)**: Standardized wrapper around TanStack Query objects. Replaces boilerplate `if (isLoading) return ...; if (isError) return ...;` across every view with:
  ```tsx
  <QueryBoundary query={tasksQuery}>
    {(data) => <TaskTable tasks={data.content} />}
  </QueryBoundary>
  ```
* **Design Token System ([tokens.css](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_FE/src/styles/tokens.css))**:
  * Implements two distinct operational palettes:
    * `field` (default daylight/outdoor operations: soft sage greens `#f3f5f0`, rich ink `#10180f`, and brand green `#3c9f40`).
    * `command` (low-light command room: dark carbon background `#0d120e`, panel green-black `#151b16`, bright phosphor green `#57b85b`).
  * Typography loads *Plus Jakarta Sans* for headings/body and *IBM Plex Mono* for task codes (`TSK-0012`), timestamps, and metric values.

---

## 3. Backend Analysis

### 3.1 Architecture & Framework Conventions
* **Framework**: Spring Boot 4.1.1 running on Java 21 LTS.
* **Persistence**: Spring Data JPA with Hibernate 7.4.5 over PostgreSQL.
* **Security**: Spring Security (Stateless filter chain, JJWT 0.12.6).
* **Validation**: Jakarta Bean Validation (`@Valid`, `@NotBlank`, `@Email`, `@Min`, `@Max`).
* **Boilerplate Reduction**: Lombok (`@Getter`, `@Setter`, `@RequiredArgsConstructor`).
* **Design Pattern**: Classical layered enterprise architecture:
  $$\text{HTTP Request} \longrightarrow \text{Controller} \longrightarrow \text{Service Layer} \longrightarrow \text{Repository (JPA)} \longrightarrow \text{PostgreSQL}$$

### 3.2 Backend Package Breakdown
```
com.throughline.taskmanagement/
├── config/       # SecurityConfig (CORS, CSRF, filter chain, PasswordEncoder)
├── controller/   # 7 REST Controllers exposing 40+ endpoints
├── dto/
│   ├── request/  # 28 Immutable Record request DTOs with validation rules
│   └── response/ # 35 Immutable Record response DTOs
├── enums/        # Role, TaskStatus, AssigneeType, TaskSeverity, NotificationType, etc.
├── exception/    # Domain exceptions & GlobalExceptionHandler
├── mapper/       # Manual entity-to-DTO mappers (TaskMapper, PersonMapper, etc.)
├── model/        # 15 JPA Entity classes
├── repository/   # 15 Spring Data JPA repositories with custom JPQL queries
├── scheduling/   # TaskStalenessJob (@Scheduled background cron)
├── security/     # JwtService, CustomUserDetailsService, CurrentPersonResolver, LoginRateLimiter
└── service/      # Service interfaces and transactional service implementations (*ServiceImpl)
```

### 3.3 Security & Authentication Infrastructure
1. **Stateless JWT Validation ([JwtAuthenticationFilter.java](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_BE/src/main/java/com/throughline/taskmanagement/security/JwtAuthenticationFilter.java))**:
   * Intercepts incoming requests, inspecting `Authorization: Bearer <token>`.
   * Invokes `JwtService.extractEmail(token)`.
   * Queries `CustomUserDetailsService.loadUserByUsername(email)`.
   * Verifies `jwtService.isTokenValid(token, userDetails.getUsername())`.
   * Sets `UsernamePasswordAuthenticationToken` into `SecurityContextHolder`.
2. **Actor Resolution ([CurrentPersonResolver.java](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_BE/src/main/java/com/throughline/taskmanagement/security/CurrentPersonResolver.java))**:
   * Controllers **do not trust client-supplied identity parameters** (such as `authorId`, `createdById`, or `actorId`).
   * Controllers pass the Spring Security `Authentication` object to `CurrentPersonResolver.resolve(authentication)`, which fetches the authentic `Person` record from the database using `authentication.getName()`.
3. **Brute-Force Protection ([LoginRateLimiter.java](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_BE/src/main/java/com/throughline/taskmanagement/security/LoginRateLimiter.java))**:
   * A thread-safe, in-memory rate limiter using `ConcurrentHashMap<String, Window>`.
   * Enforces a maximum of 5 failed attempts within a 15-minute sliding window, keyed by lowercase email address.
   * Protects `/api/v1/auth/login`, `/api/v1/auth/verify-email`, and `/api/v1/auth/reset-password`.
4. **Service-Layer Authorization Guards**:
   * Spring Security's HTTP authorization filter only verifies whether the user is authenticated (`.anyRequest().authenticated()`).
   * Fine-grained domain permissions are enforced procedurally inside service implementations using helper methods:
     * `requireDirector(Person actor, String message)`
     * `requireSuperAdmin(Person actor, String message)`
     * `requireCanReassign(Person actor, Task task)`
     * `requireCanDelete(Person actor, Task task)`
     * `requireCanViewPerson(Long viewerId, Long targetPersonId)`

### 3.4 Automated Background Processing ([TaskStalenessJob.java](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_BE/src/main/java/com/throughline/taskmanagement/scheduling/TaskStalenessJob.java))
* Enabled via `@EnableScheduling` on [ThroughlineApplication.java](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_BE/src/main/java/com/throughline/taskmanagement/ThroughlineApplication.java).
* Executes daily at 08:00 AM server time via cron: `@Scheduled(cron = "0 0 8 * * *")`.
* Queries `taskRepository.findStalledCandidates(TaskStatus.COMPLETED, threshold)` for all non-completed tasks whose `updatedAt` is older than 7 days and whose `staleAlertSentAt` is null.
* Dispatches in-app `TASK_STALLED` notifications to the accountable party (Team Leader for team tasks, assignee for subtasks, Department Head for department tasks).
* Sets `task.setStaleAlertSentAt(LocalDateTime.now())` to prevent duplicate daily spam. This flag is reset automatically as soon as genuine progress is logged.

### 3.5 Complete Backend API Endpoint Directory

| HTTP Method | Endpoint URI | Description | Authorization Level |
|---|---|---|---|
| **POST** | `/api/v1/auth/signup` | Register new user account | Public |
| **POST** | `/api/v1/auth/login` | Authenticate user & receive JWT | Public |
| **POST** | `/api/v1/auth/verify-email` | Validate 6-digit signup OTP | Public |
| **POST** | `/api/v1/auth/resend-otp` | Request new signup verification code | Public |
| **POST** | `/api/v1/auth/forgot-password` | Request 6-digit password reset code | Public |
| **POST** | `/api/v1/auth/reset-password` | Reset password using verified code | Public |
| **POST** | `/api/v1/auth/logout` | Client token invalidation acknowledgment | Public |
| **GET** | `/api/v1/auth/me` | Fetch authenticated user profile | Authenticated |
| **POST** | `/api/v1/tasks` | Create top-level task | Director / Executive / Super Admin |
| **POST** | `/api/v1/tasks/{id}/subtasks` | Create leaf or implementation subtask | Team Leader / Dept Head / Exec / Admin |
| **GET** | `/api/v1/tasks` | List tasks (scoped by role/department/assignee) | Authenticated (Server-scoped) |
| **GET** | `/api/v1/tasks/{id}` | Fetch detailed task aggregate | Authenticated |
| **GET** | `/api/v1/tasks/code/{taskCode}` | Fetch task details by code (e.g., TSK-0012) | Authenticated |
| **GET** | `/api/v1/tasks/search` | Search tasks by title or code | Authenticated (Server-scoped) |
| **PUT** | `/api/v1/tasks/{id}` | Update task title, description, dateAssigned | Authenticated |
| **DELETE** | `/api/v1/tasks/{id}` | Delete task and cascade child records | Creator Director / Executive / Admin |
| **GET** | `/api/v1/tasks/activity` | Unified task creation/deletion audit log | Director / Executive / Super Admin |
| **POST** | `/api/v1/tasks/{id}/comments` | Add dated progress comment & update % | Authenticated |
| **GET** | `/api/v1/tasks/{id}/comments` | Paginated progress comment history | Authenticated |
| **POST** | `/api/v1/tasks/{id}/discussion-comments` | Add discussion/Q&A comment (threaded) | Authenticated |
| **POST** | `/api/v1/tasks/{id}/reassign` | Reassign task with mandatory written reason | Team Leader / Dept Head / Exec / Admin |
| **GET** | `/api/v1/tasks/{id}/reassignments` | Paginated reassignment audit log | Authenticated |
| **GET** | `/api/v1/tasks/{id}/progress-timeline` | Chronological progress sparkline data | Authenticated |
| **POST** | `/api/v1/tasks/{id}/deadline-extensions` | Submit deadline extension request | Assignee / Accountable Lead / Exec |
| **PUT** | `/api/v1/tasks/{id}/deadline-extensions/{extId}` | Approve or reject extension request | Task Decider / Approver |
| **PUT** | `/api/v1/tasks/{id}/deadline-extensions/{extId}/forward` | Forward request to Executive/CEO | Intermediate Decider (Director) |
| **PUT** | `/api/v1/tasks/{id}/deadline` | Directly move deadline (self-approved) | Task Decider / Executive / Super Admin |
| **GET** | `/api/v1/tasks/{id}/deadline-extensions` | Paginated deadline change history | Authenticated |
| **GET** | `/api/v1/tasks/deadline-extensions/pending` | Requests inbox awaiting caller decision | Director / Executive / Super Admin |
| **PUT** | `/api/v1/tasks/{id}/pin` | Pin or unpin task on task board | Department Head / Executive / Admin |
| **POST** | `/api/v1/tasks/{id}/documents` | Upload supporting document (multipart) | Authenticated |
| **GET** | `/api/v1/tasks/{id}/documents/{docId}/download` | Download document byte content | Authenticated |
| **DELETE** | `/api/v1/tasks/{id}/documents/{docId}` | Remove supporting document | Document Uploader / Director+ |
| **POST** | `/api/v1/people` | Create and onboard person record | Director / Super Admin |
| **GET** | `/api/v1/people` | List people (teammates only for Member) | Authenticated (Server-scoped) |
| **GET** | `/api/v1/people/{id}` | Get person profile | Teammate / Director+ |
| **PUT** | `/api/v1/people/{id}` | Update personal information | Authenticated |
| **DELETE** | `/api/v1/people/{id}` | Delete person record | Authenticated |
| **GET** | `/api/v1/people/{id}/statistics` | Individual task metrics & team breakdown | Teammate / Director+ |
| **GET** | `/api/v1/people/{id}/tasks` | Full task involvement history | Teammate / Director+ |
| **PUT** | `/api/v1/people/{id}/role` | Promote/demote user authorization role | Super Admin only |
| **PUT** | `/api/v1/people/{id}/active` | Activate or deactivate account | Super Admin only |
| **GET** | `/api/v1/people/role-changes` | Role-change audit feed | Director / Super Admin |
| **GET** | `/api/v1/people/account-status-changes` | Account activation/lockout audit feed | Director / Super Admin |
| **POST** | `/api/v1/people/{id}/send-password-reset` | Admin-triggered password reset code | Super Admin only |
| **POST** | `/api/v1/teams` | Create new team | Director / Executive / Super Admin |
| **GET** | `/api/v1/teams` | List all teams | Authenticated |
| **GET** | `/api/v1/teams/{id}` | Get team detail | Authenticated |
| **PUT** | `/api/v1/teams/{id}` | Update team name | Authenticated |
| **DELETE** | `/api/v1/teams/{id}` | Delete team record | Authenticated |
| **GET** | `/api/v1/teams/{id}/statistics` | Team progress KPIs & member stats | Team Member / Director+ |
| **GET** | `/api/v1/teams/{id}/tasks` | Tasks assigned to this team | Team Member / Director+ |
| **GET** | `/api/v1/teams/{id}/members` | Active roster with leadership flags | Team Member / Director+ |
| **PUT** | `/api/v1/teams/{id}/leader/{personId}` | Assign team leader | Department Head / Executive / Admin |
| **POST** | `/api/v1/teams/{id}/members` | Add member to team with reason | Team Leader / Dept Head / Exec / Admin |
| **DELETE** | `/api/v1/teams/{id}/members/{personId}` | Remove member from team with reason | Team Leader / Dept Head / Exec / Admin |
| **GET** | `/api/v1/teams/{id}/membership-history` | Audit log of all team membership changes | Team Member / Director+ |
| **GET** | `/api/v1/teams/activity` | Cross-team membership activity feed | Director / Super Admin |
| **POST** | `/api/v1/departments` | Create new department | Executive / Super Admin |
| **GET** | `/api/v1/departments` | List all departments | Authenticated |
| **GET** | `/api/v1/departments/{id}` | Get department details & member teams | Authenticated |
| **PUT** | `/api/v1/departments/{id}` | Rename department | Super Admin only |
| **PUT** | `/api/v1/departments/{id}/head` | Reassign head Director | Super Admin only |
| **DELETE** | `/api/v1/departments/{id}` | Delete department (must be empty) | Executive / Super Admin |
| **GET** | `/api/v1/departments/activity` | Department deletion audit log | Director / Super Admin |
| **GET** | `/api/v1/dashboard/overview` | High-level organizational KPI totals | Authenticated |
| **GET** | `/api/v1/dashboard/status-mix` | Status breakdown percentages | Authenticated |
| **GET** | `/api/v1/dashboard/progress-over-time` | Historical day-by-day progress timeline | Authenticated |
| **GET** | `/api/v1/dashboard/team-leaderboard` | Ranked performance table of teams | Authenticated |
| **GET** | `/api/v1/dashboard/people-summary` | Aggregated performance metrics by person | Authenticated |
| **GET** | `/api/v1/dashboard/search` | Global quick search across people & tasks | Authenticated |
| **GET** | `/api/v1/dashboard/director/tasks` | "My Initiatives" task feed for Directors | Director / Executive / Super Admin |
| **GET** | `/api/v1/dashboard/executive/tasks` | Critical & Executive-assigned tasks | Executive / Super Admin |
| **GET** | `/api/v1/dashboard/executive/department-health`| Department traffic-light rollup | Executive / Super Admin |
| **GET** | `/api/v1/dashboard/executive/kpis` | Executive high-level health KPI tiles | Executive / Super Admin |
| **GET** | `/api/v1/notifications` | Paginated notification inbox for caller | Authenticated (Self only) |
| **PUT** | `/api/v1/notifications/{id}/read` | Mark individual notification as read | Authenticated (Recipient only) |
| **GET** | `/api/v1/notifications/unread-count` | Total unread notification badge count | Authenticated (Self only) |
| **GET** | `/api/v1/notifications/unread-counts-by-type`| Badge counts grouped by category | Authenticated (Self only) |
| **PUT** | `/api/v1/notifications/mark-category-read`| Batch mark notifications read by category | Authenticated (Self only) |

---

## 4. Database Analysis

### 4.1 Relational Architecture & Entity-Relationship Schema

```
 +----------------------------------------------------------------------------------------------------+
 |                                          DEPARTMENTS                                               |
 | id (PK) | name (UQ) | head_director_id (FK->persons) | created_by_id (FK->persons) | created_at    |
 +----------------------------------------------------------------------------------------------------+
       ^                     ^                                      ^
       |                     |                                      |
       | department_id       | department_id                        | assigned_department_id
       |                     |                                      |
 +-------------+      +--------------+                      +---------------------------------------+
 |   PERSONS   |      |    TEAMS     |                      |                 TASKS                 |
 | id (PK)     |      | id (PK)      |                      | id (PK)                               |
 | email (UQ)  |      | name (UQ)    |                      | task_code (UQ)                        |
 | full_name   |      | created_by   |                      | title                                 |
 | role        |      | dept_id (FK) |                      | parent_task_id (FK -> tasks.id)       |
 | password    |      +--------------+                      | assigned_by_id (FK -> persons.id)     |
 | is_active   |             ^                              | assigned_person_id (FK -> persons.id) |
 +-------------+             |                              | assigned_team_id (FK -> teams.id)     |
    ^   ^   ^                | team_id                      | assigned_dept_id (FK -> depts.id)     |
    |   |   |                |                              | assignee_type (INDIVIDUAL/TEAM/DEPT)  |
    |   |   +--------+-------+                              | progress_percentage                   |
    |   |            |                                      | status (PENDING/ONGOING/COMPLETED)    |
    |   |     +--------------+                              | deadline                              |
    |   |     | TEAM_MEMBERS |                              | severity (LOW/MED/HIGH/CRITICAL)      |
    |   |     | id (PK)      |                              | pinned (BOOLEAN)                      |
    |   |     | team_id (FK) |                              +---------------------------------------+
    |   |     | person_id(FK)|                                  |       |          |            |
    |   |     | is_leader    |                                  |       |          |            |
    |   |     +--------------+                                  |       |          |            |
    |   |                                                       |       |          |            |
    |   +--------------------------+----------------------------+       |          |            |
    |                              |                                    |          |            |
    v                              v                                    v          v            v
 +-------------------+    +--------------------+            +-------------+  +------------+  +------------+
 |   TASK_COMMENTS   |    | TASK_REASSIGNMENTS |            |  SUBTASKS   |  | EXTENSIONS |  | DOCUMENTS  |
 | id (PK)           |    | id (PK)            |            | (Recursive  |  | id (PK)    |  | id (PK)    |
 | task_id (FK)      |    | task_id (FK)       |            |  tasks      |  | task_id(FK)|  | task_id(FK)|
 | author_id (FK)    |    | from_person_id(FK) |            |  parent_id) |  | status     |  | content    |
 | percentage        |    | to_person_id (FK)  |            +-------------+  +------------+  |  (bytea)   |
 | body (text)       |    | reason (text)      |                                             +------------+
 | type (PROG/DISC)  |    +--------------------+
 +-------------------+
```

### 4.2 Detailed Table Inventory & Key Constraints

| Table Name | Primary Key | Key Foreign Keys | Unique Constraints | Indexes | Cascade Behaviors |
|---|---|---|---|---|---|
| `persons` | `id` (IDENTITY) | `department_id -> departments(id)` | `email` | Unique on `email` | Restrict on delete |
| `teams` | `id` (IDENTITY) | `department_id -> departments(id)`, `created_by_id -> persons(id)` | `name` | `idx_teams_created_by_id` | Cascades `members` in JPA |
| `team_members` | `id` (IDENTITY) | `team_id -> teams(id)`, `person_id -> persons(id)` | `(team_id, person_id)` | `idx_team_members_person_id` | JPA orphanRemoval from `teams` |
| `team_membership_changes` | `id` (IDENTITY) | `team_id`, `person_id`, `changed_by_id -> persons(id)` | None | `idx_team_membership_changes_team_id`, `idx_..._timestamp` | No cascade (permanent audit log) |
| `departments` | `id` (IDENTITY) | `head_director_id -> persons(id)`, `created_by_id -> persons(id)` | `name` | Unique on `name` | Restrict on delete |
| `department_activities` | `id` (IDENTITY) | `performed_by_id -> persons(id)` | None | None | No cascade |
| `tasks` | `id` (IDENTITY) | `parent_task_id -> tasks(id)`, `assigned_by_id -> persons(id)`, `assigned_person_id -> persons(id)`, `assigned_team_id -> teams(id)`, `assigned_department_id -> departments(id)` | `task_code` | `idx_tasks_assigned_person_id`, `idx_tasks_assigned_team_id`, `idx_tasks_assigned_by_id`, `idx_tasks_parent_task_id`, `idx_tasks_status` | JPA cascades `ALL` + orphanRemoval to `subtasks`, `comments`, `reassignments`, `documents`, `deadlineExtensionRequests` |
| `task_comments` | `id` (IDENTITY) | `task_id -> tasks(id)`, `author_id -> persons(id)`, `parent_comment_id -> task_comments(id)` | None | `idx_task_comments_task_id`, `idx_task_comments_author_id` | JPA parent delete cascades to comments |
| `task_reassignments` | `id` (IDENTITY) | `task_id -> tasks(id)`, `from_person_id`, `to_person_id`, `from_team_id`, `to_team_id`, `from_department_id`, `to_department_id`, `reassigned_by_id -> persons(id)` | None | `idx_task_reassignments_task_id`, `idx_..._from_person_id` | JPA parent delete cascades |
| `task_deadline_extension_requests` | `id` (IDENTITY) | `task_id -> tasks(id)`, `requested_by_id`, `decided_by_id`, `forwarded_by_id -> persons(id)` | None | `idx_task_deadline_ext_requests_task_id` | JPA parent delete cascades |
| `task_documents` | `id` (IDENTITY) | `task_id -> tasks(id)`, `uploaded_by_id -> persons(id)` | None | `idx_task_documents_task_id` | JPA parent delete cascades |
| `task_activities` | `id` (IDENTITY) | `performed_by_id -> persons(id)` | None | `idx_task_activities_task_code` | No cascade (permanent audit log) |
| `role_changes` | `id` (IDENTITY) | `person_id -> persons(id)`, `changed_by_id -> persons(id)` | None | `idx_role_changes_person_id` | No cascade |
| `account_status_changes`| `id` (IDENTITY) | `person_id -> persons(id)`, `changed_by_id -> persons(id)` | None | `idx_account_status_changes_person_id` | No cascade |
| `notifications` | `id` (IDENTITY) | `recipient_id -> persons(id)` | None | `idx_notifications_recipient_id`, `idx_notifications_recipient_is_read` | No cascade |

### 4.3 Database Schema Lifecycle & DDL Quirks
The project relies on Hibernate's automatic schema tool:
```properties
spring.jpa.hibernate.ddl-auto=update
```
**Architectural implications of `ddl-auto=update`:**
1. **Schema Additions Only**: Hibernate creates missing tables, adds newly declared columns, and creates foreign keys.
2. **Never Drops or Modifies**: It **never** drops deprecated columns, never renames fields, and never alters existing column types.
3. **Stale Enum Check Constraints**: When PostgreSQL creates a column mapped to a Java enum, Hibernate generates a Postgres `CHECK (column_name IN ('VALUE1', 'VALUE2'))`. When a new enum value was added in Java code (`Role.EXECUTIVE` or new `NotificationType`s), Hibernate failed to update the existing constraint. This caused silent runtime database rejections until the database administrator manually dropped the constraint via pgAdmin.
4. **No Database Migration Versioning**: The project does not currently use Flyway or Liquibase. There is no version-controlled script history for database schema state.

---

## 5. Authentication, Authorization & Security Analysis

### 5.1 End-to-End Authentication Architecture

```
Client (Browser)                Backend (Spring Boot)                Postgres DB
      |                                  |                                |
      |--- POST /api/v1/auth/login ----->|                                |
      |    {email, password}             |-- checkAllowed(email) -------->| (RateLimiter)
      |                                  |-- findByEmailIgnoreCase() ---->|
      |                                  |<-- returns Person record ------|
      |                                  |-- passwordEncoder.matches() ---| (Plaintext check!)
      |                                  |-- person.isActive() check -----|
      |                                  |-- person.isEmailVerified() ----|
      |                                  |-- generateToken(email) --------| (JJWT HS512)
      |<-- 200 OK {token, role, etc.} ---|                                |
      |                                  |                                |
      |--- GET /api/v1/auth/me --------->| (JwtAuthenticationFilter)      |
      |    Header: Bearer <token>        |-- extractEmail(token) ---------|
      |                                  |-- loadUserByUsername(email) -->| (DB Query 1)
      |                                  |-- CurrentPersonResolver ------>| (DB Query 2)
      |<-- 200 OK {PersonResponse} ------|                                |
```

### 5.2 Critical Security Vulnerabilities & Architectural Flaws

#### 1. Plain-Text Passwords in Database (`CRITICAL`)
* **Observed Code**: In [SecurityConfig.java](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_BE/src/main/java/com/throughline/taskmanagement/config/SecurityConfig.java):
  ```java
  @Bean
  public PasswordEncoder passwordEncoder() {
      return NoOpPasswordEncoder.getInstance();
  }
  ```
* **Vulnerability Analysis**: Passwords entered by users during signup and password reset are saved directly into the `persons.password` column as raw strings. Anyone who gains read access to the database (through SQL injection, database dump exposure, or compromised credentials) immediately captures all user passwords.
* **Remediation**: Swap `NoOpPasswordEncoder` with `new BCryptPasswordEncoder(12)` or `Argon2PasswordEncoder`. Because legacy accounts might have plaintext strings or old hashes, an explicit migration strategy or reset policy is required.

#### 2. Committed Secrets in Git History (`CRITICAL`)
* **Observed Context**: `application.properties` previously contained live database credentials and the JWT signing key:
  ```properties
  app.jwt.secret=34MSqY81ALQcofi8HDTgwir2rcSMCLxj7gIEGIMlklAmDnzlUJ+evnKIjMyMz7E2r+6szfrFn/slVErW/EgSrA==
  ```
* **Vulnerability Analysis**: While `.gitignore` and `git rm --cached` were subsequently applied, the secret remains visible in past commits on public remote repositories. Anyone with repository access can extract the JWT secret and forge signatures for any user ID, including Super Admins (`sub: superadmin@example.com`), completely bypassing login authentication.
* **Remediation**: The repository must be sanitized using `git-filter-repo` or BFG Repo-Cleaner. The production/local JWT secret must be immediately rotated, and secrets must be injected exclusively via operating system environment variables (e.g., `System.getenv("APP_JWT_SECRET")`).

#### 3. Broken Object-Level Authorization (BOLA/IDOR) on Task Modification (`HIGH`)
* **Observed Code**: In [TaskController.java](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_BE/src/main/java/com/throughline/taskmanagement/controller/TaskController.java) and [TaskServiceImpl.java](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_BE/src/main/java/com/throughline/taskmanagement/service/impl/TaskServiceImpl.java):
  ```java
  @PutMapping("/{id}")
  public ResponseEntity<TaskDetailResponse> updateTask(@PathVariable Long id, @Valid @RequestBody UpdateTaskRequest request) {
      return ResponseEntity.ok(taskService.updateTask(id, request));
  }
  ```
* **Vulnerability Analysis**: `updateTask` takes only the task ID and request body. It does not resolve the authenticated caller and does not check whether the user is a Director, a Team Leader, or even an assignee of the task. Any authenticated user (including an entry-level Member in an unrelated department) can issue `PUT /api/v1/tasks/{id}` and overwrite any task's `title`, `description`, and `dateAssigned`.
* **Remediation**: Pass the authenticated actor into `taskService.updateTask(id, actorId, request)` and enforce that only the task's creator, the department head, or an Executive/Super Admin can alter top-level metadata.

#### 4. Unrestricted Progress Manipulation via Direct Comments (`HIGH`)
* **Observed Code**: In [TaskServiceImpl.java](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_BE/src/main/java/com/throughline/taskmanagement/service/impl/TaskServiceImpl.java):
  `addProgressComment` validates that `percentageAtComment` is between 0 and 100, but performs **no authorization check** verifying whether the `authorId` has any assignment to the task.
* **Vulnerability Analysis**: Any authenticated user who knows or guesses a task ID can issue `POST /api/v1/tasks/{id}/comments` with `percentageAtComment: 100`, instantly marking another team's subtask as COMPLETED and artificially boosting parent task progress rollups.
* **Remediation**: Enforce that progress comments can only be submitted by the assigned individual, their team leader, or an authorized Director.

#### 5. Unauthorized User Profile Overwrite & Deletion (`HIGH`)
* **Observed Code**: In [PersonController.java](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_BE/src/main/java/com/throughline/taskmanagement/controller/PersonController.java):
  ```java
  @PutMapping("/{id}")
  public ResponseEntity<PersonResponse> updatePerson(@PathVariable Long id, @Valid @RequestBody CreatePersonRequest request) {
      return ResponseEntity.ok(personService.updatePerson(id, request));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deletePerson(@PathVariable Long id) {
      personService.deletePerson(id);
      return ResponseEntity.noContent().build();
  }
  ```
* **Vulnerability Analysis**:
  * `updatePerson` takes no authentication details. Any logged-in user can change another person's full name, email, job title, and rank by sending a PUT request to `/api/v1/people/{targetId}`. If an attacker changes a Director's email, they can lock out the Director.
  * `deletePerson` is exposed via `DELETE /api/v1/people/{id}` without any role check. Although the application philosophy states "accounts are deactivated, never deleted," this endpoint permits arbitrary account deletion. If the target person has no foreign key dependencies, they are permanently removed. If foreign key rows exist, it triggers an unhandled SQL constraint error.
* **Remediation**: Remove `deletePerson` entirely; enforce that profile updates are restricted to the account owner or a Super Admin.

#### 6. Unauthorized Team Renaming & Deletion (`MEDIUM`)
* **Observed Code**: In [TeamController.java](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_BE/src/main/java/com/throughline/taskmanagement/controller/TeamController.java):
  `updateTeam` and `deleteTeam` do not resolve the caller or check roles. Any user can rename or attempt to delete any team in the database.
* **Remediation**: Restrict team renaming and deletion to the head of the team's department or an Executive/Super Admin.

#### 7. Unrestricted Task Detail Read Access (`MEDIUM`)
* **Observed Code**: In [TaskController.java](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_BE/src/main/java/com/throughline/taskmanagement/controller/TaskController.java):
  `GET /api/v1/tasks/{id}` and `GET /api/v1/tasks/code/{taskCode}` have no scoping logic. While task list endpoints filter by role and department, direct ID lookups return full details (including comments, documents, reassignments, and extensions) to any authenticated user.
* **Remediation**: Enforce a read visibility check: if the viewer is a Member, reject requests for tasks not belonging to their team or assigned to them directly.

#### 8. Denial of Service via Database Byte Storage (`MEDIUM`)
* **Observed Code**: [TaskDocument.java](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_BE/src/main/java/com/throughline/taskmanagement/model/TaskDocument.java) stores raw file data in a `byte[] content` column (`bytea` in PostgreSQL) up to 20MB per file.
* **Vulnerability Analysis**: Multiple large file uploads consume substantial database disk space and RAM. Furthermore, loading `TaskDetailResponse` lazily fetches all documents for a task; if a task has ten 20MB files, mapped DTO construction can trigger 200MB memory allocations in the JVM, risking `OutOfMemoryError` under concurrent usage.
* **Remediation**: Offload binary document storage to an object store (e.g., AWS S3, MinIO, or local disk volume) and store only the storage metadata URI in PostgreSQL.

#### 9. Information Leakage in Global Error Handler (`LOW`)
* **Observed Code**: In [GlobalExceptionHandler.java](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_BE/src/main/java/com/throughline/taskmanagement/exception/GlobalExceptionHandler.java):
  ```java
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleGlobalException(Exception ex, HttpServletRequest request) {
      return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred: " + ex.getMessage(), request, null);
  }
  ```
* **Vulnerability Analysis**: Directly echoing `ex.getMessage()` exposes internal SQL syntax errors, table names, constraint definitions, or class paths to external callers.
* **Remediation**: Log `ex.getMessage()` on the server and return a sanitized, static error message to the client for unhandled 500 errors.

---

## 6. Feature-by-Feature Analysis

### Feature 1: User Authentication, Signup & Email Verification (OTP)
1. **Purpose**: Secure onboarding of users while validating control over their email inboxes.
2. **Frontend Implementation**: [LoginPage.tsx](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_FE/src/features/auth/LoginPage.tsx), [SignupPage.tsx](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_FE/src/features/auth/SignupPage.tsx), and [VerifyEmailPage.tsx](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_FE/src/features/auth/VerifyEmailPage.tsx). Includes email formatting regex, "Remember me" toggle, 6-digit pin entry with countdown resend timer.
3. **Backend Implementation**: [AuthController.java](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_BE/src/main/java/com/throughline/taskmanagement/controller/AuthController.java) and [AuthServiceImpl.java](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_BE/src/main/java/com/throughline/taskmanagement/service/impl/AuthServiceImpl.java). Generates cryptographically secure 6-digit numeric OTPs via `SecureRandom`. Dispatches email through [MailServiceImpl.java](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_BE/src/main/java/com/throughline/taskmanagement/service/impl/MailServiceImpl.java) using Gmail SMTP.
4. **Database Entities**: `persons` (`email`, `password`, `email_verified`, `otp_code`, `otp_expires_at`).
5. **Request Flow**:
   User submits signup $\rightarrow$ `POST /api/v1/auth/signup` $\rightarrow$ User saved with `email_verified = false` $\rightarrow$ OTP generated and emailed $\rightarrow$ User redirected to `/verify-email` $\rightarrow$ Submits code to `POST /api/v1/auth/verify-email` $\rightarrow$ `email_verified` set to true $\rightarrow$ JWT issued.
6. **Access Control**: Publicly accessible endpoints.
7. **Normal Behavior**: Account is created, user receives OTP, validates it within 10 minutes, and is authenticated.
8. **Error Behavior**:
   * Duplicate email throws `DuplicateResourceException` (HTTP 409).
   * Incorrect code triggers failure count in `LoginRateLimiter`. Exceeding 5 attempts locks the account for 15 minutes (`TooManyAttemptsException`, HTTP 429).
   * Expired OTP returns HTTP 401.
9. **Limitations**: If SMTP credentials are not configured, signup verification fails unless the administrator manually marks the record verified via direct database update.
10. **Improvement Potential**: Add webhook/test modes for local development to bypass SMTP, and implement password hashing.

### Feature 2: Task Hierarchy, Subtask Creation & Rollups
1. **Purpose**: Allow high-level initiatives to be progressively broken down into actionable subtasks while maintaining aggregate progress visibility.
2. **Frontend Implementation**: [CreateTaskModal.tsx](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_FE/src/features/tasks/components/CreateTaskModal.tsx), [SubtasksPanel.tsx](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_FE/src/features/taskDetail/components/SubtasksPanel.tsx), and [CreateSubtaskModal.tsx](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_FE/src/features/taskDetail/components/CreateSubtaskModal.tsx). Automatically calculates allowable subtask assignees based on team membership.
3. **Backend Implementation**: [TaskServiceImpl.java](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_BE/src/main/java/com/throughline/taskmanagement/service/impl/TaskServiceImpl.java).
   * Depth 0: Executive Department task or Director Team task.
   * Depth 1: Department implementation task (team/individual) or Team subtask (individual).
   * Depth 2: Leaf subtask under a Department implementation team task.
   * Rollup logic: `recalculateParentRollup()` calculates the rounded mean of child task percentages and bubbles up recursively.
4. **Database Entities**: `tasks` (`parent_task_id`, `depth`, `assignee_type`, `progress_percentage`, `status`).
5. **Request Flow**:
   Director creates task $\rightarrow$ `POST /api/v1/tasks` $\rightarrow$ Initial opening comment logged (0%) $\rightarrow$ Team Leader clicks "Add Subtask" $\rightarrow$ `POST /api/v1/tasks/{id}/subtasks` $\rightarrow$ Subtask created $\rightarrow$ Parent task rollup recalculated $\rightarrow$ Notification dispatched.
6. **Access Control**:
   * Depth 0 Department Task: Executive / Super Admin only.
   * Depth 0 Team Task: Director / Executive / Super Admin only.
   * Subtasks: Team Leader of the owning team or Department Head Director.
7. **Normal Behavior**: Adding a subtask resets parent percentage to the average of its child set.
8. **Error Behavior**: Assigning a subtask to an individual who does not belong to the parent task's team throws `InvalidAssignmentException` (HTTP 400).
9. **Limitations**: Tree depth is hardcoded to a maximum of 2.
10. **Improvement Potential**: Allow arbitrary $N$-level task hierarchies with weighted subtask contributions (e.g., story points or hours).

### Feature 3: Progress Logging & Trend Visualization
1. **Purpose**: Track verified forward momentum on individual assignments.
2. **Frontend Implementation**: [AddCommentForm.tsx](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_FE/src/features/taskDetail/components/AddCommentForm.tsx) and [TaskProgressSparkline.tsx](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_FE/src/features/taskDetail/components/TaskProgressSparkline.tsx). Recharts renders chronological SVG area/line charts.
3. **Backend Implementation**: `TaskServiceImpl.addProgressComment()`. Validates percentage boundary ($0\le p \le 100$). Generates sequential sequence numbers. For rollup tasks, `TaskMapper.buildRollupTimeline()` chronologically replays every child subtask's timeline to mathematically reconstruct the historical rollup trend at each instant.
4. **Database Entities**: `task_comments` (`task_id`, `author_id`, `percentage_at_comment`, `body`, `sequence_number`, `type`).
5. **Request Flow**: Assignee enters comment text and selects new percentage $\rightarrow$ `POST /api/v1/tasks/{id}/comments` $\rightarrow$ Comment saved $\rightarrow$ Task progress updated $\rightarrow$ Parent task recalculates rollup $\rightarrow$ Returns updated `TaskDetailResponse`.
6. **Access Control**: Open to authenticated users (intended for assignees/leads).
7. **Normal Behavior**: Percentage updates, status transitions if reaching 100%, trend sparkline re-renders.
8. **Error Behavior**: Passing a percentage outside 0–100 throws `InvalidProgressException` (HTTP 400).
9. **Limitations**: Does not prevent backward progress logging (e.g., regressing from 80% to 20%), though comments explain why.
10. **Improvement Potential**: Enforce explicit approval workflows for downward progress revisions.

### Feature 4: Discussion Threads & Nested Replies
1. **Purpose**: Allow contextual Q&A on tasks without corrupting the mathematical progress log.
2. **Frontend Implementation**: [DiscussionPanel.tsx](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_FE/src/features/taskDetail/components/DiscussionPanel.tsx). Renders collapsible reply boxes and 1-level indented threads.
3. **Backend Implementation**: `TaskServiceImpl.addDiscussionComment()`. Distinguishes `CommentType.DISCUSSION` from `CommentType.PROGRESS`. Flattens nested replies to depth 1 (Instagram model).
4. **Database Entities**: `task_comments` (`type = 'DISCUSSION'`, `parent_comment_id`).
5. **Request Flow**: User types question $\rightarrow$ `POST /api/v1/tasks/{id}/discussion-comments` $\rightarrow$ Comment logged without altering task percentage $\rightarrow$ Notification sent to task author/assignee.
6. **Access Control**: Open to all authenticated users.
7. **Normal Behavior**: Thread displays author name, timestamp, and body.
8. **Error Behavior**: Replying to a non-existent parent comment ID throws `ResourceNotFoundException`.
9. **Limitations**: Plain text only; no Markdown parsing or file attachments directly inside discussion comments.
10. **Improvement Potential**: Rich text/Markdown formatting, `@mentions` with automated notification tagging.

### Feature 5: Deadline Extensions & Chain-of-Command Approvals
1. **Purpose**: Formalize requests for additional time, preventing quiet deadline slippage.
2. **Frontend Implementation**: [RequestExtensionModal.tsx](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_FE/src/features/taskDetail/components/RequestExtensionModal.tsx), [ExtendDeadlineModal.tsx](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_FE/src/features/taskDetail/components/ExtendDeadlineModal.tsx), and [RequestsPage.tsx](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_FE/src/features/dashboard/RequestsPage.tsx).
3. **Backend Implementation**:
   * `requestDeadlineExtension()`: Validates that the requested deadline is in the future and later than the current deadline.
   * `decideDeadlineExtension()`: Handles approval/rejection.
   * `forwardExtensionRequestToApprover()`: When a task is originated by the CEO/Executive, an intermediate Director cannot approve it; they can only reject it or forward it to the Executive inbox.
4. **Database Entities**: `task_deadline_extension_requests` (`status`, `current_deadline`, `requested_deadline`, `justification`, `decided_by_id`, `decision_note`, `forwarded_at`).
5. **Request Flow**:
   Assignee requests date $\rightarrow$ `POST /api/v1/tasks/{id}/deadline-extensions` $\rightarrow$ Decider views request in `/requests` $\rightarrow$ Calls `PUT .../decide` $\rightarrow$ If approved, `task.deadline` is updated $\rightarrow$ Requester notified.
6. **Access Control**: Requesting is restricted to accountable assignees; deciding is restricted to the chain-of-command Director/Executive.
7. **Normal Behavior**: Extension history displays old vs. new dates, justifications, and decision notes.
8. **Error Behavior**: Non-decider attempting to decide returns HTTP 403 Forbidden.
9. **Limitations**: Requests cannot be edited once submitted; they can only be approved or rejected.
10. **Improvement Potential**: Counter-proposal workflows (e.g., approver granting 3 days instead of requested 7).

### Feature 6: Task Reassignment with Mandatory Justification
1. **Purpose**: Maintain an unbroken chain of custody when tasks are handed off between individuals, teams, or departments.
2. **Frontend Implementation**: [ReassignTaskModal.tsx](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_FE/src/features/taskDetail/components/ReassignTaskModal.tsx). Dynamically adjusts selection targets based on task type.
3. **Backend Implementation**: `TaskServiceImpl.reassignTask()`. Enforces that subtasks can only be reassigned to individuals within the same parent team. Department tasks can only be moved between departments by Executives. Appends record to `TaskReassignment`.
4. **Database Entities**: `task_reassignments` (`from_assignee_type`, `to_assignee_type`, `from_person_id`, `to_person_id`, `reason`).
5. **Request Flow**: Lead selects new assignee and inputs justification $\rightarrow$ `POST /api/v1/tasks/{id}/reassign` $\rightarrow$ Assignee pointer updated $\rightarrow$ Reassignment audit record saved $\rightarrow$ Previous and new assignees notified.
6. **Access Control**: Team Leader, Department Head, Executive, or Super Admin.
7. **Normal Behavior**: Task card reflects new owner; reassignment panel displays full historical chain.
8. **Error Behavior**: Blank reason fails Jakarta `@NotBlank` validation (HTTP 400).
9. **Limitations**: Reassigning a top-level team task does not automatically reassign its existing leaf subtasks.
10. **Improvement Potential**: Batch reassignment options when an individual departs a team.

### Feature 7: Supporting Document Management
1. **Purpose**: Attach formal memos, requirements documents, and specifications directly to tasks.
2. **Frontend Implementation**: [DocumentsPanel.tsx](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_FE/src/features/taskDetail/components/DocumentsPanel.tsx). File picker with progress indicator and download trigger.
3. **Backend Implementation**:
   * `addDocument()`: Validates file size ($\le 20\text{MB}$) and MIME type against allowlist (PDF, Word, Excel, PowerPoint, PNG, JPEG, Plain Text).
   * `downloadDocument()`: Streams byte content with `Content-Disposition: attachment`.
   * `deleteDocument()`: Allowed only by the uploader or a Director+.
4. **Database Entities**: `task_documents` (`file_name`, `content_type`, `file_size`, `content` as `bytea`).
5. **Request Flow**: Multipart upload $\rightarrow$ `POST /api/v1/tasks/{id}/documents` $\rightarrow$ Stored in Postgres $\rightarrow$ UI updates file roster.
6. **Access Control**: Any task viewer can upload/download; only uploader or Director can delete.
7. **Normal Behavior**: Document appears with file extension icon, size, and uploader attribution.
8. **Error Behavior**: Uploading executable (.exe, .sh) or unrecognized MIME types returns HTTP 400.
9. **Limitations**: 20MB files stored directly in database tables impact backup sizes and query performance.
10. **Improvement Potential**: Cloud storage integration (S3/GCS) with presigned download URLs.

### Feature 8: Department & Org-Chart Architecture
1. **Purpose**: Model macro-level organizational structure above teams.
2. **Frontend Implementation**: [DepartmentsListPage.tsx](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_FE/src/features/departments/DepartmentsListPage.tsx) and [DepartmentPage.tsx](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_FE/src/features/departments/DepartmentPage.tsx).
3. **Backend Implementation**: [DepartmentServiceImpl.java](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_BE/src/main/java/com/throughline/taskmanagement/service/impl/DepartmentServiceImpl.java). Deleting a department is permitted only if zero people, zero teams, and zero tasks remain associated with it.
4. **Database Entities**: `departments`, `department_activities`.
5. **Request Flow**: Executive creates department $\rightarrow$ `POST /api/v1/departments` $\rightarrow$ Head Director assigned $\rightarrow$ Teams and users can now be assigned.
6. **Access Control**: Read is open to all authenticated users; creation is Executive+; renaming and head changes are Super Admin only.
7. **Normal Behavior**: Displays department head, child teams, and active tasks.
8. **Error Behavior**: Attempting to delete a department containing active teams throws `InvalidAssignmentException` (HTTP 400).
9. **Limitations**: Departments cannot be nested hierarchically (no sub-departments or divisions).
10. **Improvement Potential**: Multi-level department trees and budget/headcount tracking.

### Feature 9: Multi-Team Membership & Team Leadership
1. **Purpose**: Model realistic organizational assignments where people participate in multiple project teams while potentially leading one.
2. **Frontend Implementation**: [TeamsListPage.tsx](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_FE/src/features/teams/TeamsListPage.tsx) and [TeamPage.tsx](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_FE/src/features/teams/TeamPage.tsx).
3. **Backend Implementation**: [TeamServiceImpl.java](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_BE/src/main/java/com/throughline/taskmanagement/service/impl/TeamServiceImpl.java). Uses `TeamMember` join entity. `isLeader` is a property of the join row, allowing a user to be Leader in Team A while serving as a regular Member in Team B.
4. **Database Entities**: `teams`, `team_members`, `team_membership_changes`.
5. **Request Flow**: Director creates team $\rightarrow$ Adds members via `POST /api/v1/teams/{id}/members` with mandatory justification $\rightarrow$ Reassigns leader via `PUT .../leader/{personId}` $\rightarrow$ Every change logged in `team_membership_changes`.
6. **Access Control**: Team creation requires Director+; membership modification requires Team Leader or Department Head.
7. **Normal Behavior**: Full roster rendered with Leader badges and join dates.
8. **Error Behavior**: Duplicate membership attempt throws `DuplicateResourceException`.
9. **Limitations**: Teams cannot have co-leaders (strictly capped at 1 leader per team).
10. **Improvement Potential**: Support for secondary team leads / technical leads.

### Feature 10: People Directory & Individual Performance Metrics
1. **Purpose**: Provide visibility into personnel, job titles, ranks, and track records.
2. **Frontend Implementation**: [PeopleListPage.tsx](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_FE/src/features/people/PeopleListPage.tsx) and [PersonProfilePage.tsx](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_FE/src/features/people/PersonProfilePage.tsx). Members see `MyTeammatesGrid`; Directors see the full company directory.
3. **Backend Implementation**: `PersonServiceImpl.getPersonStatistics()`. Calculates average progress, tasks completed, tasks ongoing, tasks handed off, and a per-team performance breakdown.
4. **Database Entities**: `persons`, `team_members`, `tasks`.
5. **Request Flow**: User visits profile $\rightarrow$ `GET /api/v1/people/{id}/statistics` $\rightarrow$ Aggregates computed in Java $\rightarrow$ UI renders stat cards and involvement history.
6. **Access Control**: Directors can view anyone; Members can view only themselves and teammates.
7. **Normal Behavior**: Displays individual KPIs and team contributions.
8. **Error Behavior**: Member requesting profile of someone outside their teams receives HTTP 403 Forbidden.
9. **Limitations**: Performance statistics calculate simple averages without task complexity or deadline weightings.
10. **Improvement Potential**: On-time completion rates and velocity metrics.

### Feature 11: Governance, Role Changes & Account Deactivation
1. **Purpose**: Provide Super Admins with tools to govern security roles and account access while preventing accidental system lockout.
2. **Frontend Implementation**: [PersonAdminControls.tsx](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_FE/src/features/people/components/PersonAdminControls.tsx). Role selector dropdown and deactivation toggle with required reason field.
3. **Backend Implementation**: `PersonServiceImpl.changeRole()` and `PersonServiceImpl.setActive()`. Enforces that the final remaining active Super Admin cannot be demoted or deactivated.
4. **Database Entities**: `role_changes`, `account_status_changes`.
5. **Request Flow**: Super Admin selects new role and inputs justification $\rightarrow$ `PUT /api/v1/people/{id}/role` $\rightarrow$ Role updated $\rightarrow$ Audit entry written to `role_changes` $\rightarrow$ Target user notified.
6. **Access Control**: Strictly Super Admin only.
7. **Normal Behavior**: User permissions change immediately; next API call by the user adopts new role claims.
8. **Error Behavior**: Attempting to demote or deactivate the last Super Admin throws `InvalidAssignmentException` (HTTP 400).
9. **Limitations**: Deactivated users' active tokens continue to work until their next request, at which point `CustomUserDetailsService` rejects them.
10. **Improvement Potential**: Immediate token revocation via an in-memory or Redis-backed token denylist.

### Feature 12: Unified Activity Audit Feed
1. **Purpose**: Comprehensive compliance timeline recording every significant organizational event.
2. **Frontend Implementation**: [ActivityPage.tsx](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_FE/src/features/tasks/ActivityPage.tsx). Includes segmented control filtering (`ALL`, `TASK`, `ROLE`, `STATUS`, `DEPARTMENT`).
3. **Backend Implementation**: Four separate paged endpoints merged on the client side:
   * `taskService.getTaskActivity()`
   * `personService.getRoleChangeActivity()`
   * `personService.getAccountStatusChangeActivity()`
   * `departmentService.getDepartmentActivity()`
4. **Database Entities**: `task_activities`, `role_changes`, `account_status_changes`, `department_activities`.
5. **Request Flow**: Director navigates to `/activity` $\rightarrow$ Frontend fires 4 parallel queries $\rightarrow$ Results normalized and sorted by timestamp descending $\rightarrow$ Displayed in paginated stream.
6. **Access Control**: Director, Executive, and Super Admin only.
7. **Normal Behavior**: Chronological activity cards with actor name, timestamp, and action description.
8. **Error Behavior**: Member attempting to visit `/activity` is redirected to `/`.
9. **Limitations**: Fetches up to 100 items per log and merges in browser memory. Events beyond 100 in any single table are truncated from the merged view.
10. **Improvement Potential**: Implement a unified database view or polymorphic audit table with true server-side pagination.

### Feature 13: In-App Notification System & Navigation Badges
1. **Purpose**: Notify users of actions affecting them and provide visual unread counters in the navigation sidebar.
2. **Frontend Implementation**: [NotificationBell.tsx](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_FE/src/features/notifications/components/NotificationBell.tsx) and `useNavBadgeCounts` in [Sidebar.tsx](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_FE/src/components/layout/Sidebar.tsx).
3. **Backend Implementation**: [NotificationServiceImpl.java](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_BE/src/main/java/com/throughline/taskmanagement/service/impl/NotificationServiceImpl.java). Broadcasts notifications to affected individuals. Provides `countUnreadByType` grouping unread counts per `NotificationType`.
4. **Database Entities**: `notifications` (`recipient_id`, `type`, `message`, `related_entity_id`, `is_read`).
5. **Request Flow**: Task assigned $\rightarrow$ Notification inserted $\rightarrow$ Recipient's top bar displays unread badge count $\rightarrow$ User clicks notification $\rightarrow$ Marked as read via `PUT .../read` $\rightarrow$ Navigates directly to task/profile.
6. **Access Control**: Users can read only their own notifications.
7. **Normal Behavior**: Unread counter decrements; clicking a notification routes directly to the underlying resource.
8. **Error Behavior**: Accessing another person's notification returns HTTP 404/403.
9. **Limitations**: Long-polling or manual refetching only; no WebSockets or Server-Sent Events (SSE).
10. **Improvement Potential**: Real-time push updates via Spring WebSocket / STOMP.

### Feature 14: Executive & Director Dashboards
1. **Purpose**: Provide high-level organizational intelligence, progress trend lines, status mix ratios, and team rankings.
2. **Frontend Implementation**: [DashboardPage.tsx](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_FE/src/features/dashboard/DashboardPage.tsx), [KpiRow.tsx](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_FE/src/features/dashboard/components/KpiRow.tsx), [ExecutiveKpiRow.tsx](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_FE/src/features/dashboard/components/ExecutiveKpiRow.tsx), [ProgressOverTimeChart.tsx](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_FE/src/features/dashboard/components/ProgressOverTimeChart.tsx), [StatusDonut.tsx](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_FE/src/features/dashboard/components/StatusDonut.tsx), and [TeamLeaderboardTable.tsx](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_FE/src/features/dashboard/components/TeamLeaderboardTable.tsx).
3. **Backend Implementation**: [DashboardServiceImpl.java](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_BE/src/main/java/com/throughline/taskmanagement/service/impl/DashboardServiceImpl.java).
4. **Database Entities**: `tasks`, `teams`, `persons`, `task_comments`, `departments`.
5. **Request Flow**: Dashboard mounts $\rightarrow$ Multiple API endpoints invoked in parallel (`/overview`, `/status-mix`, `/progress-over-time`, `/team-leaderboard`, `/executive/kpis`) $\rightarrow$ Rendered into charts and KPI tiles.
6. **Access Control**: Members see `MyDashboardSummary`; Directors see team and departmental views; Executives see org-wide executive health.
7. **Normal Behavior**: Interactive Recharts components, tooltips, responsive data tables.
8. **Error Behavior**: Failed sub-queries show isolated error states without crashing the entire dashboard.
9. **Limitations**: `DashboardServiceImpl` performs in-memory aggregations across all tasks using `taskRepository.findAll()`.
10. **Improvement Potential**: Push aggregations into SQL `GROUP BY` and window functions, or use database materialized views.

### Feature 15: Global Header Search
1. **Purpose**: Rapid search across people and tasks from any page in the application.
2. **Frontend Implementation**: [SearchInput.tsx](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_FE/src/features/search/components/SearchInput.tsx) and [SearchResultsPage.tsx](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_FE/src/features/search/SearchResultsPage.tsx). Includes 300ms debounce hook (`useDebounce`) and popup dropdown.
3. **Backend Implementation**: `DashboardServiceImpl.globalSearch()`. Runs parallel case-insensitive `ILIKE` queries against task titles, task codes, person names, and job titles.
4. **Database Entities**: `persons`, `tasks`.
5. **Request Flow**: User types in header $\rightarrow$ Debounced 300ms $\rightarrow$ `GET /api/v1/dashboard/search?q=...` $\rightarrow$ Dropdown shows top 5 people and tasks $\rightarrow$ Pressing Enter navigates to `/search?q=...`.
6. **Access Control**: Available to all authenticated users.
7. **Normal Behavior**: Instant preview with keyboard escape and click-outside dismissal.
8. **Error Behavior**: Empty or whitespace query does not trigger API requests.
9. **Limitations**: Full-table substring scan (`%q%`) prevents standard B-tree index utilization.
10. **Improvement Potential**: PostgreSQL Full-Text Search (`tsvector` / `tsquery`) or Trigram indexes (`pg_trgm`).

---

## 7. Code Quality & Architecture

### 7.1 Separation of Concerns & Clean Architecture
* **Frontend**: Highly structured according to feature-sliced principles. Each domain feature encapsulates its own API calls (`api/`), components (`components/`), hooks (`hooks/`), and CSS modules (`*.module.css`). UI components in `components/ui/` are reusable, decoupled, and do not import domain models.
* **Backend**: Follows standard Spring Controller-Service-Repository tiers. Data Transfer Objects (DTOs) are modeled as immutable Java `record`s with Jakarta validation annotations. Domain entities are not exposed directly to the REST interface.

### 7.2 Code Duplication & Consistency Issues
* **Duplicate Authorization Logic**: Chain-of-command resolution logic (`resolveDeadlineDecider`) is duplicated verbatim in [TaskServiceImpl.java](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_BE/src/main/java/com/throughline/taskmanagement/service/impl/TaskServiceImpl.java), [TaskMapper.java](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_BE/src/main/java/com/throughline/taskmanagement/mapper/TaskMapper.java), and [NotificationServiceImpl.java](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_BE/src/main/java/com/throughline/taskmanagement/service/impl/NotificationServiceImpl.java). If chain-of-command rules change, updates must be made in three places.
* **God Service Anti-Pattern**: [TaskServiceImpl.java](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_BE/src/main/java/com/throughline/taskmanagement/service/impl/TaskServiceImpl.java) is 1,353 lines of code (75KB). It manages task CRUD, subtask creation, progress comments, discussion threading, reassignments, deadline requests, deadline approvals, deadline extensions, task pinning, document uploads, document downloads, and activity logging.
* **Manual Mapper Verbosity**: [TaskMapper.java](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_BE/src/main/java/com/throughline/taskmanagement/mapper/TaskMapper.java) spans 350 lines, manually mapping dozens of record parameters by hand. A mapper generator like MapStruct would eliminate boilerplate and prevent parameter mismatch bugs.

### 7.3 Frontend/Backend Coupling & Assumptions
* **Hardcoded Ports**: The frontend hardcodes `baseURL: 'http://localhost:8080'` in [axiosClient.ts](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_FE/src/api/axiosClient.ts). It does not read from Vite environment variables (`import.meta.env.VITE_API_URL`), making containerization (Docker) or staging deployment impossible without code edits.
* **Client-Side Role Gate Mismatches**: In [DepartmentPage.tsx](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_FE/src/features/departments/DepartmentPage.tsx), `<DepartmentAdminControls>` is rendered if `isExecutive` is true. However, the backend methods `renameDepartment` and `changeDepartmentHead` explicitly reject anyone who is not `SUPER_ADMIN`. As a result, an Executive is shown buttons that predictably fail with 403 Forbidden.

---

## 8. Performance & Scalability Analysis

### 8.1 In-Memory Aggregations & Out-of-Memory Risks
In [DashboardServiceImpl.java](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_BE/src/main/java/com/throughline/taskmanagement/service/impl/DashboardServiceImpl.java):
```java
List<Task> tasks = taskRepository.findAll();
long totalTasks = tasks.size();
long completedCount = tasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();
```
* **Performance Impact**: Calling `taskRepository.findAll()` pulls every single task row in the database into JVM heap memory.
* **Progress Over Time Scalability**: In `getProgressOverTime(LocalDate from, LocalDate to)`, the code loops through every single day in the date range, and for every day, iterates through all tasks, filtering each task's `comments` collection in memory.
   $$\text{Complexity} = O(D \times T \times C)$$
   Where $D$ is the number of days, $T$ is the total number of tasks, and $C$ is the average number of comments per task. At 1,000 tasks and a 90-day range, this executes hundreds of thousands of operations in memory per request.

### 8.2 The N+1 Database Query Problem
Several service methods suffer from severe N+1 query patterns:
1. **Team Leaderboard**: `DashboardServiceImpl.getTeamLeaderboard()` executes `teamRepository.findAll()`. For each team ($N$), it fires:
   * `taskRepository.findByAssignedTeamId(team.getId())` (Query $N$)
   * `teamMemberRepository.findByTeamIdAndIsLeaderTrue(team.getId())` (Query $N$)
   Total queries = $1 + 2N$.
2. **People Summary**: `DashboardServiceImpl.getPeopleSummary()` executes `personRepository.findAll()`. For each person, it fires `taskRepository.findByAssignedPersonId(person.getId())`. Total queries = $1 + N$.
3. **Global Search**: `DashboardServiceImpl.globalSearch()` searches people and tasks. For every matching person, it queries their team memberships, and for each membership, queries their assigned tasks on that team. For every matching task, it queries `taskCommentRepository.findFirstByTaskIdOrderByCreatedAtDesc()`.
4. **Task Detail Lazy Loading**: In [TaskRepository.java](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_BE/src/main/java/com/throughline/taskmanagement/repository/TaskRepository.java), `findWithDetailsById` eagerly fetches only `comments` to avoid Hibernate's `MultipleBagFetchException`. When `TaskMapper.toDetailResponse` runs, navigating `task.getSubtasks()`, `task.getReassignments()`, `task.getDeadlineExtensionRequests()`, and `task.getDocuments()` triggers 4 additional separate SQL queries per task view.

### 8.3 Hibernate Bag Semantics & Collection Mapping
In [Task.java](file:///c:/Users/HP/Desktop/UNI/Internship/Task_Tracker/TaskTracker_BE/src/main/java/com/throughline/taskmanagement/model/Task.java), collections (`subtasks`, `comments`, `reassignments`, `documents`, `deadlineExtensionRequests`) are declared as `java.util.List`:
```java
@OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
private List<TaskComment> comments = new ArrayList<>();
```
In Hibernate, mapping `@OneToMany` to a `List` creates a **Bag**. Hibernate cannot eagerly fetch two or more bags simultaneously in a single query via `JOIN FETCH` because it would create a Cartesian product that corrupts collection row mapping (throwing `MultipleBagFetchException`). Changing these collections to `java.util.Set` allows multiple collections to be safely fetched together without Cartesian multiplication errors.

---

## 9. Current Limitations & Potential Problems

### Confirmed Issues
1. **Plaintext Passwords**: Stored as plain text via `NoOpPasswordEncoder`.
2. **Leaked Git Credentials**: `app.jwt.secret` and local database passwords exist in historical git commits.
3. **Broken Authorization on Tasks**: `PUT /api/v1/tasks/{id}` allows any authenticated user to modify any task's title and dates.
4. **Broken Authorization on Progress**: `POST /api/v1/tasks/{id}/comments` allows any user to alter any subtask's progress percentage.
5. **Broken Authorization on People**: `PUT /api/v1/people/{id}` and `DELETE /api/v1/people/{id}` allow any user to modify or delete person records.
6. **Broken Authorization on Teams**: `PUT /api/v1/teams/{id}` and `DELETE /api/v1/teams/{id}` allow arbitrary team renaming and deletion.
7. **Hardcoded API URL**: Frontend `axiosClient.ts` hardcodes `http://localhost:8080`.
8. **UI/Backend Permission Discrepancy**: Department admin controls (rename/head change) are displayed to Executives on the frontend, but rejected with 403 by the backend.

### Potential Risks
1. **JVM Memory Pressure from File Blobs**: Storing up to 20MB files inside PostgreSQL `bytea` columns and mapping them into byte arrays on task retrieval creates severe heap pressure.
2. **In-Memory Dashboard Bottleneck**: `taskRepository.findAll()` in `DashboardServiceImpl` will cause severe latency or out-of-memory crashes as task count exceeds several thousand rows.
3. **In-Memory Rate Limiting in Clustered Environments**: `LoginRateLimiter` uses an in-memory `ConcurrentHashMap`. If the backend is deployed across multiple instances or containers behind a load balancer, brute-force attempts will be divided among nodes, weakening lockout enforcement.
4. **Notification Orphan Routing**: Deleting a task cascades deletes to subtasks and comments, but notifications store `relatedEntityId` as a raw `Long`. Clicking an old notification for a deleted task routes the user to a 404 error page.

### Design Trade-offs
1. **Sequential Task Code Generation**: `nextTaskCode()` queries `MAX(SUBSTRING(task_code FROM 5))` to find the highest sequence number. Under high-concurrency task creation, two transactions could generate the same sequence number, causing one to fail on the database unique constraint.
2. **Client-Side Merged Activity Feed**: Merging four audit feeds in browser memory avoids complex cross-table union queries on the backend, but limits visibility to the latest 100 entries per category.

---

## 10. Recommended Improvements — Without Making Changes

### Priority 1: Security & Identity Governance
1. **Implement Secure Password Hashing**:
   * *Layer*: Backend.
   * *Action*: Replace `NoOpPasswordEncoder` with `BCryptPasswordEncoder(12)` in `SecurityConfig.java`. Provide a database migration script or forced password reset workflow for existing accounts.
   * *Impact*: Protects credentials from database leaks and unauthorized exposure.
2. **Rotate and Externalize Secrets**:
   * *Layer*: Backend configuration & Git.
   * *Action*: Scrub git history of `application.properties` using `git-filter-repo`. Externalize `app.jwt.secret`, database credentials, and mail passwords into environment variables.
3. **Lock Down Broken Object-Level Authorization (BOLA)**:
   * *Layer*: Backend controllers & services.
   * *Action*: Resolve authenticated caller on `updateTask`, `addProgressComment`, `updatePerson`, `deletePerson`, `updateTeam`, and `deleteTeam`. Enforce strict ownership or role checks before modifying entities.

### Priority 2: Performance & Scalability Optimization
1. **Replace In-Memory Dashboard Aggregations with SQL Aggregations**:
   * *Layer*: Backend repository & service.
   * *Action*: Replace `taskRepository.findAll()` in `DashboardServiceImpl` with JPQL aggregate queries:
     ```sql
     SELECT new com.throughline...DashboardOverviewResponse(
         COALESCE(AVG(t.progressPercentage), 0.0),
         COUNT(t),
         COUNT(CASE WHEN t.status = 'COMPLETED' THEN 1 END),
         COUNT(CASE WHEN t.status = 'ONGOING' THEN 1 END),
         COUNT(CASE WHEN t.status = 'PENDING' THEN 1 END)
     ) FROM Task t
     ```
   * *Impact*: Eliminates memory overhead; execution speed improves by orders of magnitude.
2. **Eliminate N+1 Queries in Leaderboards & Search**:
   * *Layer*: Backend repository.
   * *Action*: Use SQL joins with `GROUP BY` to aggregate team task counts and average progress in a single query rather than issuing $2N$ queries.
3. **Externalize Document Storage**:
   * *Layer*: Backend & Database.
   * *Action*: Transition `TaskDocument` to store a file path/URL pointing to an S3-compatible bucket or dedicated disk volume rather than storing binary byte arrays in PostgreSQL.

### Priority 3: Architecture & Maintainability
1. **Decompose `TaskServiceImpl`**:
   * *Layer*: Backend service.
   * *Action*: Split the 1,353-line class into specialized domain services: `TaskLifecycleService`, `TaskProgressService`, `TaskDeadlineService`, `TaskDocumentService`, and `TaskReassignmentService`.
2. **Environment Variable Configuration on Frontend**:
   * *Layer*: Frontend.
   * *Action*: Update `axiosClient.ts` to use `import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'`.
3. **Introduce Database Migration Versioning**:
   * *Layer*: Database & Backend.
   * *Action*: Add Flyway or Liquibase to manage versioned SQL migration scripts (`V1__init.sql`, `V2__add_exec_role.sql`) and disable `hibernate.ddl-auto=update`.

---

## 11. Architecture & Data Flow Walkthroughs

### Workflow 1: Logging Progress on a Leaf Subtask

```
[User Action in UI]
  Assignee opens TaskDetailPage (/tasks/42)
  Types: "Integrated payment webhook callbacks"
  Selects: 75%
  Clicks: "Log progress"
         |
         v
[Frontend Component & Hook]
  AddCommentForm.tsx triggers useAddComment() mutation
         |
         v
[API Request]
  POST http://localhost:8080/api/v1/tasks/42/comments
  Headers: Authorization: Bearer <JWT>
  Body: { percentageAtComment: 75, body: "Integrated payment webhook callbacks" }
         |
         v
[Spring Boot Security & Controller]
  JwtAuthenticationFilter validates token -> sets SecurityContext
  TaskController.addProgressComment()
  CurrentPersonResolver extracts actorId from JWT
  Passes AddCommentRequest to TaskService
         |
         v
[Service Layer Processing]
  TaskServiceImpl.addProgressComment(taskId=42, request):
  1. Loads Task (id=42) from TaskRepository
  2. Validates percentage (0 <= 75 <= 100)
  3. Creates TaskComment:
       percentageAtComment = 75, body = "...", sequenceNumber = 4, type = PROGRESS
  4. Updates Task: progressPercentage = 75, status = ONGOING, staleAlertSentAt = null
  5. Identifies Parent Task (id=10)
  6. Invokes recalculateParentRollup(parent):
       subtasks = findByParentTaskId(10) -> computes average (e.g. (75 + 25) / 2 = 50%)
       parent.setProgressPercentage(50)
       parent.setStatus(ONGOING)
  7. If Parent Task has grandparent (Department Task), bubbles rollup recursively
         |
         v
[Repository & Database Operations]
  taskCommentRepository.save(comment)   --> INSERT INTO task_comments ...
  taskRepository.save(task)              --> UPDATE tasks SET progress_percentage=75, status='ONGOING' ...
  taskRepository.save(parent)            --> UPDATE tasks SET progress_percentage=50, status='ONGOING' ...
         |
         v
[Response & Frontend Re-render]
  TaskMapper maps updated Task into TaskDetailResponse
  Returns HTTP 200 OK
  TanStack Query invalidates ['task', 42], ['tasks'], ['dashboard']
  UI re-renders: Progress bar moves to 75%, Sparkline plots new point, Comment appears in log
```

### Workflow 2: Requesting, Forwarding & Deciding a Deadline Extension

```
[Step 1: Extension Request]
  Assignee clicks "Request extension" on TSK-0015
  Submits: Requested Date = 2026-10-15, Justification = "Awaiting third-party security clearance"
  -> POST /api/v1/tasks/15/deadline-extensions
  -> TaskServiceImpl verifies requestedDate > currentDate
  -> Inserts TaskDeadlineExtensionRequest (status = PENDING)
  -> Dispatches in-app notification to task's Director

[Step 2: Chain-of-Command Review & Forwarding]
  Director opens Requests inbox (/requests)
  Task was originated under a CEO Executive mandate:
    Director has authority to reject, but cannot approve CEO-mandated extensions
  Director clicks "Forward to Executive"
  -> PUT /api/v1/tasks/15/deadline-extensions/8/forward
  -> TaskServiceImpl sets forwardedBy = Director, forwardedAt = NOW()
  -> Dispatches notification to Executive / CEO

[Step 3: Executive Approval]
  Executive opens Requests inbox -> Sees forwarded request from Director
  Clicks "Approve" with note: "Approved. Prioritize external audit."
  -> PUT /api/v1/tasks/15/deadline-extensions/8
     Body: { approve: true, decisionNote: "Approved. Prioritize external audit." }
  -> TaskServiceImpl updates:
       request.status = APPROVED
       request.decidedBy = Executive
       request.decidedAt = NOW()
       task.deadline = 2026-10-15
  -> Saves Task & Request
  -> Dispatches DEADLINE_EXTENSION_APPROVED notification to Assignee & Director
  -> Frontend cache updates; new deadline displays across all task boards
```

### Workflow 3: Reassigning a Team Task with Mandatory Justification

```
[User Action in UI]
  Team Leader opens TSK-0008 (Subtask under "Core Banking Migration")
  Clicks "Reassign" button -> Opens ReassignTaskModal
  Selects new team member: "Lt. Col. Emmanuel"
  Inputs Reason: "Shifted to lead database partitioning phase"
         |
         v
[API Request]
  POST /api/v1/tasks/8/reassign
  Headers: Authorization: Bearer <JWT>
  Body: { newPersonId: 14, reason: "Shifted to lead database partitioning phase" }
         |
         v
[Backend Validation & Execution]
  TaskController resolves actorId from JWT
  TaskServiceImpl.reassignTask():
  1. requireCanReassign(): Verifies actor is Team Leader of task's parent team
  2. Verifies Lt. Col. Emmanuel is an active member of the parent task's team
  3. Creates TaskReassignment:
       fromPerson = Maj. Musoni (ID 9)
       toPerson = Lt. Col. Emmanuel (ID 14)
       reason = "Shifted to lead database partitioning phase"
       reassignedBy = Team Leader (actorId)
       reassignedAt = NOW()
  4. Updates task.assignedPerson = Lt. Col. Emmanuel
  5. Saves Task & Reassignment record
  6. Dispatches SUBTASK_REASSIGNED notifications to Emmanuel and Musoni
         |
         v
[Database & Response]
  INSERT INTO task_reassignments ...
  UPDATE tasks SET assigned_person_id = 14 ...
  Returns updated TaskDetailResponse (HTTP 200 OK)
  UI updates owner badge and appends entry to Reassignment History audit timeline
```

---

## 12. Project Maturity Assessment

| Subsystem / Area | Maturity Level | Evaluation & Codebase Evidence |
|---|---|---|
| **Domain Model & Invariants** | `Mature / Production-Ready` | Progress-only-via-comments, automatic status calculation, XOR assignment constraints, and recursive rollup bubble logic are cleanly designed and enforced. |
| **Audit Logging** | `Mature / Production-Ready` | Dedicated append-only tables (`TaskActivity`, `RoleChange`, `AccountStatusChange`, `TeamMembershipChange`, `DepartmentActivity`, `TaskReassignment`) preserve historical continuity. |
| **Frontend UI/UX & Theming** | `High Quality / Complete` | Pure CSS tokens, field/command theme modes, responsive sidebar/tab bar, Recharts sparklines, and unified `QueryBoundary` error handling create an excellent user experience. |
| **Automated Test Coverage** | `Partially Complete` | Backend has unit/integration tests covering repository queries, authorization checks, and rollup math. Frontend has **zero automated tests** (no test runner or testing framework installed). |
| **Authentication & Core Security**| `Fragile / Pre-Alpha` | Plaintext passwords via `NoOpPasswordEncoder` and committed JWT signing secrets in git history preclude any real-world deployment until overhauled. |
| **Object-Level Authorization** | `Fragile / Incomplete` | Missing authorization checks on `PUT /api/v1/tasks/{id}`, `POST .../comments`, `PUT .../people/{id}`, `DELETE .../people/{id}`, `PUT .../teams/{id}`, and `DELETE .../teams/{id}` leave the API vulnerable to BOLA. |
| **Database Operations & Migrations**| `Experimental / Fragile` | Reliance on `hibernate.ddl-auto=update` without Flyway/Liquibase causes stale enum check constraints. Storing 20MB file blobs in `bytea` causes JVM memory spikes. |
| **Dashboard Query Performance** | `Fragile at Scale` | In-memory aggregations via `taskRepository.findAll()` and N+1 queries in leaderboards and search will degrade rapidly under real-world data volumes. |

---

## 13. Final Improvement Roadmap

```
Phase 1: Security Hardening (Immediate)
├── Replace NoOpPasswordEncoder with BCryptPasswordEncoder
├── Cleanse git repository history & rotate JWT secrets
├── Secure BOLA endpoints (Task update, Comment progress, Person/Team update & delete)
└── Fix frontend/backend permission mismatch in DepartmentAdminControls

Phase 2: Database & Architecture Hardening (Short-Term)
├── Introduce Flyway database migrations (replace ddl-auto=update)
├── Move document storage out of Postgres bytea to external object storage
├── Externalize API base URL in frontend via Vite environment variables
└── Fix Notification orphan links on task deletion

Phase 3: Performance & Scalability (Medium-Term)
├── Push Dashboard aggregations into native SQL queries (eliminate findAll)
├── Eliminate N+1 queries in Team Leaderboards, People Summary, and Global Search
├── Convert JPA List bags to Sets to resolve Hibernate MultipleBagFetchException
└── Implement database indexes for full-text search (tsvector / trigram)

Phase 4: Advanced Features & Maintainability (Long-Term)
├── Decompose TaskServiceImpl into focused domain services
├── Implement WebSockets (STOMP) for real-time notifications and dashboard updates
├── Set up Vitest & React Testing Library for frontend automated test coverage
└── Add multi-level department hierarchies and configurable subtask contribution weights
```

### Categorized Action Items

#### 1. Critical & Security Issues
* **Password Hashing**: Implement BCrypt hashing.
  * *Reason*: Storing plaintext passwords violates fundamental security standards and exposes all users if the database is compromised.
* **Secret Rotation**: Re-generate JWT secret and move all credentials to environment variables.
  * *Reason*: The existing secret was committed to public git history and can be used to forge administrator tokens.
* **Object-Level Authorization Guards**: Add caller validation to task updates, progress comment submissions, team edits, and user profile endpoints.
  * *Reason*: Prevents standard users from modifying unauthorized resources via direct API calls.

#### 2. Backend & Database Improvements
* **Adopt Flyway Migrations**: Replace `hibernate.ddl-auto=update` with versioned migration scripts.
  * *Reason*: Eliminates broken enum check constraints and provides deterministic, reproducible schema upgrades across environments.
* **Externalize File Storage**: Transition `TaskDocument` from PostgreSQL `bytea` to cloud object storage or volume storage.
  * *Reason*: Eliminates JVM heap exhaustion risks and prevents massive database backup bloat.
* **Refactor Monolithic Services**: Break down `TaskServiceImpl` into modular services.
  * *Reason*: Improves code maintainability, testability, and reduces merge conflicts.

#### 3. Frontend Improvements
* **Environment Variable Configuration**: Make the API base URL configurable via `import.meta.env.VITE_API_URL`.
  * *Reason*: Enables building Docker containers and deploying to staging/production servers.
* **Automated Frontend Testing**: Install Vitest, React Testing Library, and MSW (Mock Service Worker).
  * *Reason*: Ensures complex UI state, permission gates, and data transformations do not regress during refactoring.
* **Align Role Gates**: Restrict `<DepartmentAdminControls>` to `isSuperAdmin`.
  * *Reason*: Prevents Executives from seeing action buttons that the backend will reject with 403 Forbidden.

#### 4. Performance & Scalability Enhancements
* **Database-Side Aggregations**: Rewrite dashboard queries to use SQL `COUNT`, `AVG`, and `GROUP BY`.
  * *Reason*: Reduces query execution time from seconds to milliseconds and prevents out-of-memory crashes as dataset size increases.
* **Collection Mapping Refactor**: Change `@OneToMany` collections on `Task` from `List` to `Set`.
  * *Reason*: Allows Hibernate to safely join-fetch multiple collections without triggering `MultipleBagFetchException`.
* **Distributed Rate Limiting**: Move `LoginRateLimiter` storage from JVM memory to Redis.
  * *Reason*: Ensures rate limits are enforced consistently across multiple clustered application instances.
