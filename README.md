# Throughline

Throughline is a task-progress tracking system built around one rule: **every percentage
is justified by a dated comment, and every reassignment is justified by a reason.** It's
themed around a Rwandan military/banking hierarchy (ranks, Directors, teams), but
underneath that theming it's a general-purpose "who's doing what, and how far along is
it, and who said so" tracker.

This README covers the whole system — both the Spring Boot backend and the React
frontend — how to run it on your own machine, how its configuration works, and what must
never end up in version control.

## Contents

- [What it does](#what-it-does)
- [Tech stack](#tech-stack)
- [Project structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Running it locally, step by step](#running-it-locally-step-by-step)
- [Configuration reference](#configuration-reference)
- [Roles & permissions](#roles--permissions)
- [Security — what must never be committed](#security--what-must-never-be-committed)
- [Known limitations](#known-limitations)
- [Where to look next](#where-to-look-next)

## What it does

- **Task hierarchy.** A top-level task is always assigned to a whole *team* (only a
  Director or Super Admin can create one). A team can break its task into *subtasks*,
  each assigned to one *individual* member of that team (the team's own Leader can do
  this too, not just a Director/Super Admin).
- **Progress only moves one way.** A task's percentage never gets edited directly — it
  changes only when someone adds a dated progress comment, and the task's status
  (Pending/Ongoing/Completed) is derived from that percentage automatically.
- **Reassignment needs a reason, and isn't open to everyone.** Reassigning a task
  requires a mandatory written reason, and is restricted to a Director, a Super Admin, or
  that specific task's Team Leader — an ordinary team member can't do it.
- **Teams.** A person can belong to more than one team at once. Each team has exactly one
  Leader. Every membership change (added/removed) is logged with who did it and why.
- **People & roles.** Three global roles, ascending: `MEMBER < DIRECTOR < SUPER_ADMIN`.
  "Team Leader" is a *separate*, per-team concept — a Member can lead one team and be a
  plain member of another at the same time. See [Roles & permissions](#roles--permissions).
- **Auth.** JWT-based login/signup. A brand-new signup has to verify their email with a
  one-time code sent by real Gmail SMTP before they can log in. Forgot your password?
  There's a self-service reset-by-email-code flow, plus a Super-Admin-triggered version
  of the same flow for when someone's locked out and can't request it themselves.
- **Visibility is role-scoped, server-side.** A Member logging in only ever sees: tasks
  assigned to them directly or to a team they belong to; only their own teammates on the
  People page; and, for a team they aren't on, just its name and who leads it — nothing
  else. Directors and Super Admins see everything, everywhere.
- **Notifications.** In-app notifications for role changes, account (de)activation, team
  membership changes, and admin-triggered password resets — always sent to the affected
  person, not the person who made the change.
- **Director/Super Admin dashboard.** Org-wide KPIs, a progress-over-time trend line, a
  status-mix donut, a team leaderboard, a people summary, and a "my initiatives" section
  for tasks that Director personally created.
- **Global search** across people and tasks from the top bar, from anywhere in the app.
- **Audit trail.** A Super-Admin-only page lists every role change ever made, org-wide.
- **Accounts are deactivated, never deleted** — a Super Admin can lock an account without
  losing that person's task/comment/reassignment history.

## Tech stack

**Backend** — Java 21 · Spring Boot 4.1.1 · Spring Security (JWT via `jjwt` 0.12.6) ·
Spring Data JPA / Hibernate 7.4.5 · PostgreSQL · Maven (wrapper included, no separate
Maven install needed).

**Frontend** — React 19 · TypeScript · Vite · React Router v6 · TanStack Query for all
server state · Axios · Recharts for charts · plain CSS Modules (no Tailwind, no component
library).

## Project structure

```
Task_Tracker/
├── TaskTracker_BE/    Spring Boot API — runs on http://localhost:8080
└── TaskTracker_FE/    React app (Vite) — runs on http://localhost:5173
```

They're two independent projects that only talk to each other over HTTP — there's no
shared build step, no monorepo tooling. Run each with its own toolchain, in its own
terminal.

## Prerequisites

- **Java 21** (JDK)
- **Node.js** 18+ and npm
- **PostgreSQL**, running locally (or reachable), with a database you'll create in step 1
- *(Optional — needed only for real outgoing email)* a Gmail account with an **App
  Password** generated for it (not your normal Gmail password — see step 2 below)

## Running it locally, step by step

### 1. Create the database

```sql
CREATE DATABASE task_tracker_db;
```

That's the only manual schema step. Hibernate's `ddl-auto=update` creates and updates
every table automatically from the JPA entities the first time the backend boots — there
are no migration files to run.

### 2. Create your two local config files

Both of these are **gitignored** — a fresh clone doesn't come with either one, on
purpose, since together they hold every real local credential the app needs. Create both:

`TaskTracker_BE/src/main/resources/application.properties`
```properties
spring.application.name=Throughline

spring.datasource.url=jdbc:postgresql://localhost:5432/task_tracker_db
spring.datasource.username=postgres
spring.datasource.password=your_postgres_password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect

# A long, random string — this signs every login token. Generate one with, e.g.:
#   openssl rand -base64 64
# (any long random string works; it doesn't have to be Base64 specifically)
app.jwt.secret=paste_a_long_random_string_here
app.jwt.expiration-ms=86400000

# Mail (Gmail SMTP) — host/port/auth flags are not secret, so they live here; the real
# credentials (spring.mail.username/password) live in the second file below, imported
# optionally so the app still boots without it (just without mail-sending capability).
spring.config.import=optional:classpath:application-secrets.properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# Where the frontend lives — used only to build links inside invite/notification emails.
app.frontend-url=http://localhost:5173
```

`TaskTracker_BE/src/main/resources/application-secrets.properties`
```properties
spring.mail.username=your_gmail_address@gmail.com
spring.mail.password=your_16_character_gmail_app_password
```

A couple of things worth being precise about here:
- Only `application-secrets.properties` (the mail credentials) is genuinely optional at
  runtime — leave it out entirely and the app still boots fine, sending an email just
  fails silently (logged, swallowed) instead of crashing. `application.properties`
  itself is not optional: no datasource credentials means no database connection, which
  means Spring fails to start.
- **Gmail App Password**, not your real Gmail password: turn on 2-Step Verification on
  the Google account, then generate one at
  [myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords). It's a
  16-character code with no spaces.

### 3. Bootstrap the first Super Admin

There's no self-service way to create a Super Admin through the app itself — signup
always creates a Member, and even a Director creating a new person can only ever hand
them Member by default (only an *existing* Super Admin can promote anyone to
Director/Super Admin). So the very first one has to be inserted directly, after the
backend has booted at least once (so the `persons` table and its columns already exist):

```sql
INSERT INTO persons (full_name, email, password, job_title, rank, role, email_verified, active, created_at)
SELECT 'Your Name', 'you@example.com', 'ChooseAPassword123',
       'System Administrator', NULL, 'SUPER_ADMIN', true, true, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM persons WHERE email = 'you@example.com'
);
```

That password is stored exactly as typed — see
[Security notes](#security--what-must-never-be-committed) for why, and don't reuse a
real password of yours here.

### 4. Run the backend

```bash
cd TaskTracker_BE
./mvnw spring-boot:run          # macOS/Linux
mvnw.cmd spring-boot:run        # Windows
```

Starts on `http://localhost:8080`.

### 5. Run the frontend

```bash
cd TaskTracker_FE
npm install
npm run dev
```

Starts on `http://localhost:5173`. It talks to the backend at a **hardcoded**
`http://localhost:8080` (`src/api/axiosClient.ts`) — the backend has to be running on
exactly that port; there's no environment variable to point it elsewhere yet.

### 6. Log in

Go to `http://localhost:5173/login` and sign in with the Super Admin email/password from
step 3. From there, use the People page to create Directors and ordinary Members —
whoever you create gets an invite email (if mail is configured) telling them to sign up
at that same email address to activate their own account.

## Configuration reference

Split across the two gitignored files from step 2 above.

### `application.properties`

| Property | What it does |
|---|---|
| `spring.application.name` | App name, cosmetic only |
| `spring.datasource.url` | Which database to connect to (host/port/db name) |
| `spring.datasource.username` / `spring.datasource.password` | Your local Postgres credentials |
| `spring.datasource.driver-class-name` / `spring.jpa.database-platform` | PostgreSQL driver/dialect |
| `spring.jpa.hibernate.ddl-auto=update` | Auto-creates/updates tables from the entities. Only ever **adds** structure — never drops or alters an existing column/constraint (see [Known limitations](#known-limitations)) |
| `spring.jpa.show-sql` | Logs every SQL statement — handy in dev, noisy at scale |
| `app.jwt.secret` | Signs every login token. **If this leaks, anyone who has it can forge a valid login as any user, including a Super Admin** — see the security note below |
| `app.jwt.expiration-ms` | How long a login token stays valid, in milliseconds (currently 24 hours) |
| `spring.config.import` | Pulls in `application-secrets.properties`, without failing if it's absent |
| `spring.mail.host` / `.port` / `.properties.mail.smtp.*` | Gmail SMTP connection settings — not secret, just config |
| `app.frontend-url` | Used only to build clickable links inside invite/notification emails |

### `application-secrets.properties`

| Property | What it does |
|---|---|
| `spring.mail.username` / `spring.mail.password` | The Gmail address + App Password that sends OTP/invite/notification/reset emails — the only properties in either file that are genuinely optional at runtime |

## Roles & permissions

| Role | Can do |
|---|---|
| **Member** | See only: tasks assigned to them directly, tasks assigned to any team they belong to, their own teammates on the People page, and (for teams they aren't on) just the team's name and Leader — nothing more |
| **Director** | Everything a Member sees, plus: create teams, create top-level tasks (always team-assigned), create new people (capped at Member role by default), full org-wide visibility on People/Teams/Tasks, their own "My Initiatives" dashboard |
| **Super Admin** | Everything a Director can do, plus: promote/demote anyone to Director or Super Admin, deactivate/reactivate any account, view the org-wide role-change audit log, trigger a password-reset email for someone else |
| **Team Leader** *(not a global role)* | Scoped **per team** — someone can lead one team and be a plain Member of another at the same time. A team's Leader can create subtasks under that team's tasks and reassign that team's tasks, the same authority a Director/Super Admin has, but only for their own team |

## Security — what must never be committed

- `TaskTracker_BE/src/main/resources/application.properties` — gitignored, and was also
  explicitly untracked from git (`git rm --cached`) after already having been committed
  for a while — see the warning below for why that untracking alone isn't a complete fix.
- `TaskTracker_BE/src/main/resources/application-secrets.properties` — already gitignored
  from the start, never committed.
- `TaskTracker_BE/seed-data/` — already gitignored; contains real names/emails used during
  development.
- Anything containing a database password, the JWT signing secret, a Gmail App Password,
  or real personal data of any kind.
- **Passwords are stored in plain text in this project** — a deliberate, explicit choice
  (see the `NoOpPasswordEncoder` comment in `SecurityConfig.java`). That means anyone with
  database access can read every password directly. That's an acceptable trade-off for a
  local/internal/demo tool; it is **not** acceptable if this is ever exposed to real users
  or the public internet — swapping back to `new BCryptPasswordEncoder()` is a one-line
  change in `SecurityConfig`, and nothing else in the auth code needs to change either
  way.

> **⚠️ Already-exposed secret, at the time of writing.** `application.properties` was
> tracked in git from the very first commit, carrying the real JWT signing secret and
> database password in plain text — and this repository has already been pushed to a
> public GitHub remote. It's since been untracked (`git rm --cached`, alongside adding it
> to `.gitignore`) so it stops being committed **going forward** — but that change only
> takes effect once it's actually committed, and even then it does nothing to the *past*:
> every old commit still has the real values in it, recoverable by anyone who looks at
> this repo's history. Rotating `app.jwt.secret` (swapping in a freshly generated random
> string) is strongly recommended before relying on this project for anything beyond
> local development — it would immediately invalidate every existing login token
> (everyone, including you, would need to log in again). Fully scrubbing the old values
> out of git history itself (e.g. with `git filter-repo`) is a further, more disruptive
> step beyond that — it rewrites every commit hash, so anyone else with a clone would need
> to re-clone rather than pull.

## Known limitations

- **No automated test suite.** Only the default Spring Boot boilerplate test exists on
  the backend; there's nothing on the frontend.
- **Forgot-password / reset-password only work for a real, deliverable email address.**
  Seeded test accounts using a fake domain (`@example.com`) can never actually receive a
  reset code — the request itself will still "succeed" (by design, to avoid leaking which
  emails are registered), but no email ever arrives.
- **An account that signed up before this project switched to plain-text passwords is
  permanently locked out of login**, since its stored value is an old BCrypt hash and
  `NoOpPasswordEncoder` does a raw string comparison — no plaintext a person types will
  ever equal a bcrypt hash again. The only fix is an admin directly overwriting that
  person's `password` column via SQL.
- **`ddl-auto=update` only ever adds schema — it never alters or drops anything
  existing.** Twice in this project's history, adding a new value to a Java enum (`Role`,
  `NotificationType`) required manually dropping a stale Postgres `CHECK` constraint in
  pgAdmin, because Hibernate had generated that constraint against the enum's *old,
  shorter* list of values and will never update it on its own. If a newly added enum value
  fails to insert with a constraint violation, this finds the culprit:
  ```sql
  SELECT conname, pg_get_constraintdef(oid)
  FROM pg_constraint
  WHERE conrelid = 'TABLE_NAME_HERE'::regclass AND contype = 'c';
  ```

## Where to look next

- `TaskTracker_BE/README.md` — an early curl-based API reference. Parts of it (especially
  the People/Teams request & response shapes) reflect an earlier version of the API, from
  before the task hierarchy/multi-team/auth rework — treat it as historical background,
  not a current source of truth.
- `TaskTracker_FE/README.md` — the frontend's folder structure and conventions. Still
  accurate, aside from a couple of notes calling the People/Teams pages "read-only" —
  they've since grown full admin/management UI (create person, role/deactivation
  controls, team creation, add/remove members).
