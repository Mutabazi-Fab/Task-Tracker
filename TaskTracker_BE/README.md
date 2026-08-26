# Throughline Task Management API

A task-progress tracking system where every percentage is justified by a dated
comment and every reassignment is justified by a reason.

## Prerequisites

The application expects a local PostgreSQL database named `task_tracker_db`.
If it doesn't exist yet, create it first:

```sql
CREATE DATABASE task_tracker_db;
```

`spring.jpa.hibernate.ddl-auto=update` will generate/update the schema on startup.

---

## Endpoints & cURL Examples

All routes are prefixed with `/api/v1`. Every example below assumes the app is
running on `http://localhost:8080`.

---

### 1. Tasks API (`/api/v1/tasks`)

**POST `/api/v1/tasks`** — create a task (always opens with comment #1)
```bash
curl -X POST http://localhost:8080/api/v1/tasks \
-H "Content-Type: application/json" \
-d '{
  "title": "Setup PostgreSQL Database",
  "description": "Initialize the db for the new microservice",
  "assignedById": 1,
  "assigneeType": "INDIVIDUAL",
  "assignedPersonId": 2,
  "dateAssigned": "2026-08-26",
  "openingNote": "Starting the initial setup, currently at 0%."
}'
```
*201 Created*
```json
{
  "id": 1,
  "taskCode": "TSK-0001",
  "title": "Setup PostgreSQL Database",
  "description": "Initialize the db for the new microservice",
  "assigneeName": "Alice Smith",
  "assigneeId": 2,
  "assigneeType": "INDIVIDUAL",
  "status": "PENDING",
  "progressPercentage": 0,
  "dateAssigned": "2026-08-26",
  "assignedByName": "Bob Lead",
  "assignedById": 1,
  "comments": [
    {
      "id": 1,
      "sequenceNumber": 1,
      "authorName": "Bob Lead",
      "percentageAtComment": 0,
      "body": "Starting the initial setup, currently at 0%.",
      "createdAt": "2026-08-26T09:00:00"
    }
  ],
  "reassignments": [],
  "progressTimeline": [
    { "percentage": 0, "date": "2026-08-26T09:00:00", "commentId": 1 }
  ],
  "createdAt": "2026-08-26T09:00:00",
  "updatedAt": "2026-08-26T09:00:00"
}
```

**GET `/api/v1/tasks`** — list, with pagination and optional status filter (returns last comment only)
```bash
curl -X GET "http://localhost:8080/api/v1/tasks?status=ONGOING&page=0&size=10"
```
*200 OK*
```json
{
  "content": [
    {
      "id": 1,
      "taskCode": "TSK-0001",
      "title": "Setup PostgreSQL Database",
      "assigneeName": "Alice Smith",
      "assigneeType": "INDIVIDUAL",
      "status": "ONGOING",
      "progressPercentage": 50,
      "dateAssigned": "2026-08-26",
      "assignedByName": "Bob Lead",
      "reassignmentCount": 0,
      "lastComment": {
        "id": 2,
        "sequenceNumber": 2,
        "authorName": "Alice Smith",
        "percentageAtComment": 50,
        "body": "Halfway done, tables are created but indexes are missing.",
        "createdAt": "2026-08-26T11:00:00"
      }
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "number": 0,
  "size": 10
}
```

**GET `/api/v1/tasks/{id}`** — full detail (complete comment + reassignment history)
```bash
curl -X GET http://localhost:8080/api/v1/tasks/1
```
*200 OK* — same shape as the create response above, with the full `comments`,
`reassignments`, and `progressTimeline` arrays populated.

**GET `/api/v1/tasks/code/{taskCode}`** — lookup by TSK-code
```bash
curl -X GET http://localhost:8080/api/v1/tasks/code/TSK-0001
```
*200 OK* — same shape as `GET /{id}`.

**GET `/api/v1/tasks/search?q=`** — search by taskCode or title (case-insensitive)
```bash
curl -X GET "http://localhost:8080/api/v1/tasks/search?q=postgres"
```
*200 OK*
```json
[
  {
    "id": 1,
    "taskCode": "TSK-0001",
    "title": "Setup PostgreSQL Database",
    "assigneeName": "Alice Smith",
    "assigneeType": "INDIVIDUAL",
    "status": "ONGOING",
    "progressPercentage": 50,
    "dateAssigned": "2026-08-26",
    "assignedByName": "Bob Lead",
    "reassignmentCount": 0,
    "lastComment": {
      "id": 2,
      "sequenceNumber": 2,
      "authorName": "Alice Smith",
      "percentageAtComment": 50,
      "body": "Halfway done, tables are created but indexes are missing.",
      "createdAt": "2026-08-26T11:00:00"
    }
  }
]
```

**PUT `/api/v1/tasks/{id}`** — update title/description/dateAssigned ONLY (never progress, status, or assignee)
```bash
curl -X PUT http://localhost:8080/api/v1/tasks/1 \
-H "Content-Type: application/json" \
-d '{
  "title": "Setup PostgreSQL Database (revised)",
  "description": "Initialize the db and seed reference data",
  "dateAssigned": "2026-08-27"
}'
```
*200 OK* — full `TaskDetailResponse` with the updated fields; `progressPercentage`
and `status` are untouched.

**DELETE `/api/v1/tasks/{id}`** — delete a task
```bash
curl -X DELETE http://localhost:8080/api/v1/tasks/1
```
*204 No Content*

**POST `/api/v1/tasks/{id}/comments`** — add a progress comment (the ONLY way progress moves)
```bash
curl -X POST http://localhost:8080/api/v1/tasks/1/comments \
-H "Content-Type: application/json" \
-d '{
  "authorId": 2,
  "percentageAtComment": 50,
  "body": "Halfway done, tables are created but indexes are missing."
}'
```
*200 OK* — full `TaskDetailResponse`; note `progressPercentage` is now `50` and
`status` has been recalculated to `"ONGOING"`.

**GET `/api/v1/tasks/{id}/comments`** — full comment history, oldest first
```bash
curl -X GET http://localhost:8080/api/v1/tasks/1/comments
```
*200 OK*
```json
[
  {
    "id": 1,
    "sequenceNumber": 1,
    "authorName": "Bob Lead",
    "percentageAtComment": 0,
    "body": "Starting the initial setup, currently at 0%.",
    "createdAt": "2026-08-26T09:00:00"
  },
  {
    "id": 2,
    "sequenceNumber": 2,
    "authorName": "Alice Smith",
    "percentageAtComment": 50,
    "body": "Halfway done, tables are created but indexes are missing.",
    "createdAt": "2026-08-26T11:00:00"
  }
]
```

**POST `/api/v1/tasks/{id}/reassign`** — reassign with a mandatory reason
```bash
curl -X POST http://localhost:8080/api/v1/tasks/1/reassign \
-H "Content-Type: application/json" \
-d '{
  "newAssigneeType": "TEAM",
  "newTeamId": 1,
  "reassignedById": 1,
  "reason": "Needs more hands, handing off to the whole Platform team."
}'
```
*200 OK* — full `TaskDetailResponse` with `assigneeType: "TEAM"` and a new entry
in `reassignments`. Reassigning to the current owner, or omitting `reason`,
returns *400 Bad Request*.

**GET `/api/v1/tasks/{id}/reassignments`** — reassignment history
```bash
curl -X GET http://localhost:8080/api/v1/tasks/1/reassignments
```
*200 OK*
```json
[
  {
    "id": 1,
    "fromName": "Alice Smith",
    "toName": "Platform",
    "reassignedByName": "Bob Lead",
    "reason": "Needs more hands, handing off to the whole Platform team.",
    "reassignedAt": "2026-08-26T12:00:00"
  }
]
```

**GET `/api/v1/tasks/{id}/progress-timeline`** — that task's percentage over its comment history
```bash
curl -X GET http://localhost:8080/api/v1/tasks/1/progress-timeline
```
*200 OK*
```json
[
  { "percentage": 0, "date": "2026-08-26T09:00:00", "commentId": 1 },
  { "percentage": 50, "date": "2026-08-26T11:00:00", "commentId": 2 }
]
```

---

### 2. People API (`/api/v1/people`)

**POST `/api/v1/people`** — create a person
```bash
curl -X POST http://localhost:8080/api/v1/people \
-H "Content-Type: application/json" \
-d '{
  "fullName": "Alice Smith",
  "email": "alice@example.com",
  "role": "Backend engineer",
  "teamId": 1
}'
```
*201 Created*
```json
{
  "id": 2,
  "fullName": "Alice Smith",
  "email": "alice@example.com",
  "role": "Backend engineer",
  "teamName": "Platform",
  "teamId": 1
}
```

**GET `/api/v1/people`** — list all people
```bash
curl -X GET http://localhost:8080/api/v1/people
```
*200 OK*
```json
[
  { "id": 2, "fullName": "Alice Smith", "email": "alice@example.com", "role": "Backend engineer", "teamName": "Platform", "teamId": 1 }
]
```

**GET `/api/v1/people/{id}`** — get one person
```bash
curl -X GET http://localhost:8080/api/v1/people/2
```
*200 OK* — same shape as create response.

**PUT `/api/v1/people/{id}`** — update a person
```bash
curl -X PUT http://localhost:8080/api/v1/people/2 \
-H "Content-Type: application/json" \
-d '{
  "fullName": "Alice A. Smith",
  "email": "alice@example.com",
  "role": "Senior backend engineer",
  "teamId": 1
}'
```
*200 OK* — updated `PersonResponse`.

**DELETE `/api/v1/people/{id}`** — delete a person
```bash
curl -X DELETE http://localhost:8080/api/v1/people/2
```
*204 No Content*

**GET `/api/v1/people/{id}/statistics`** — personal stats
```bash
curl -X GET http://localhost:8080/api/v1/people/2/statistics
```
*200 OK*
```json
{
  "averageProgress": 62.5,
  "tasksAssigned": 4,
  "tasksCompleted": 1,
  "tasksOngoing": 2,
  "tasksPending": 1,
  "commentsLogged": 9,
  "tasksHandedOff": 1,
  "fullyCompleted": false
}
```

**GET `/api/v1/people/{id}/tasks`** — task history with involvement labels
```bash
curl -X GET http://localhost:8080/api/v1/people/2/tasks
```
*200 OK*
```json
[
  { "taskId": 1, "taskCode": "TSK-0001", "title": "Setup PostgreSQL Database", "involvementLabel": "CURRENT_OWNER" },
  { "taskId": 5, "taskCode": "TSK-0005", "title": "Migrate audit logs", "involvementLabel": "PREVIOUSLY_ASSIGNED" },
  { "taskId": 7, "taskCode": "TSK-0007", "title": "Add retry queue", "involvementLabel": "COMMENTER_ONLY" }
]
```

**PUT `/api/v1/people/{id}/team/{teamId}`** — assign a person to a team
```bash
curl -X PUT http://localhost:8080/api/v1/people/2/team/1
```
*200 OK* — `PersonResponse` with the new `teamName`/`teamId`.

---

### 3. Teams API (`/api/v1/teams`)

**POST `/api/v1/teams`** — create a team
```bash
curl -X POST http://localhost:8080/api/v1/teams \
-H "Content-Type: application/json" \
-d '{
  "name": "Platform"
}'
```
*201 Created*
```json
{ "id": 1, "name": "Platform", "leaderName": null, "leaderId": null, "memberCount": 0 }
```

**GET `/api/v1/teams`** — list all teams
```bash
curl -X GET http://localhost:8080/api/v1/teams
```
*200 OK*
```json
[ { "id": 1, "name": "Platform", "leaderName": "Bob Lead", "leaderId": 1, "memberCount": 3 } ]
```

**GET `/api/v1/teams/{id}`** — get one team
```bash
curl -X GET http://localhost:8080/api/v1/teams/1
```
*200 OK* — same shape as above.

**PUT `/api/v1/teams/{id}`** — update a team
```bash
curl -X PUT http://localhost:8080/api/v1/teams/1 \
-H "Content-Type: application/json" \
-d '{
  "name": "Platform Engineering",
  "teamLeaderId": 1
}'
```
*200 OK* — updated `TeamResponse`.

**DELETE `/api/v1/teams/{id}`** — delete a team
```bash
curl -X DELETE http://localhost:8080/api/v1/teams/1
```
*204 No Content*

**GET `/api/v1/teams/{id}/statistics`** — team stats
```bash
curl -X GET http://localhost:8080/api/v1/teams/1/statistics
```
*200 OK*
```json
{
  "averageProgress": 47.5,
  "taskCount": 4,
  "memberCount": 3,
  "completedCount": 1,
  "memberProgresses": [
    { "name": "Alice Smith", "averageProgress": 62.5 },
    { "name": "Charlie Dev", "averageProgress": 30.0 }
  ]
}
```

**GET `/api/v1/teams/{id}/tasks`** — every task currently assigned to the team
```bash
curl -X GET http://localhost:8080/api/v1/teams/1/tasks
```
*200 OK*
```json
[
  {
    "id": 3,
    "taskCode": "TSK-0003",
    "title": "Set up CI pipeline",
    "assigneeName": "Platform",
    "assigneeType": "TEAM",
    "status": "ONGOING",
    "progressPercentage": 40,
    "dateAssigned": "2026-08-20",
    "assignedByName": "Bob Lead",
    "reassignmentCount": 1,
    "lastComment": {
      "id": 8,
      "sequenceNumber": 3,
      "authorName": "Charlie Dev",
      "percentageAtComment": 40,
      "body": "Pipeline builds green, deploy step still manual.",
      "createdAt": "2026-08-25T10:00:00"
    }
  }
]
```

**PUT `/api/v1/teams/{id}/leader/{personId}`** — set the team leader (must already be a member)
```bash
curl -X PUT http://localhost:8080/api/v1/teams/1/leader/1
```
*200 OK* — `TeamResponse` with the new `leaderName`/`leaderId`. *400 Bad Request*
if the person is not a member of that team.

**POST `/api/v1/teams/{id}/members/{personId}`** — add a member
```bash
curl -X POST http://localhost:8080/api/v1/teams/1/members/2
```
*200 OK* — `TeamResponse` with the incremented `memberCount`.

**DELETE `/api/v1/teams/{id}/members/{personId}`** — remove a member
```bash
curl -X DELETE http://localhost:8080/api/v1/teams/1/members/2
```
*200 OK* — `TeamResponse` with the decremented `memberCount` (and the leader
cleared if that member was the leader).

---

### 4. Dashboard API (`/api/v1/dashboard`)

**GET `/api/v1/dashboard/overview`**
```bash
curl -X GET http://localhost:8080/api/v1/dashboard/overview
```
*200 OK*
```json
{
  "orgAverageProgress": 54.3,
  "totalTasks": 12,
  "completedCount": 2,
  "ongoingCount": 7,
  "pendingCount": 3
}
```

**GET `/api/v1/dashboard/status-mix`** — feeds the donut chart
```bash
curl -X GET http://localhost:8080/api/v1/dashboard/status-mix
```
*200 OK*
```json
[
  { "status": "PENDING", "count": 3, "percentageShare": 25.0 },
  { "status": "ONGOING", "count": 7, "percentageShare": 58.33 },
  { "status": "COMPLETED", "count": 2, "percentageShare": 16.67 }
]
```

**GET `/api/v1/dashboard/progress-over-time?from=&to=`** — feeds the trend line, ascending date order
```bash
curl -X GET "http://localhost:8080/api/v1/dashboard/progress-over-time?from=2026-08-02&to=2026-08-26"
```
*200 OK*
```json
[
  { "date": "2026-08-02", "averagePercentage": 12.5 },
  { "date": "2026-08-03", "averagePercentage": 12.5 },
  { "date": "2026-08-26", "averagePercentage": 54.3 }
]
```

**GET `/api/v1/dashboard/team-leaderboard`**
```bash
curl -X GET http://localhost:8080/api/v1/dashboard/team-leaderboard
```
*200 OK*
```json
[
  { "name": "Platform", "leaderName": "Bob Lead", "averageProgress": 47.5, "taskCount": 4, "completedCount": 1 },
  { "name": "Mobile", "leaderName": "Dana Chen", "averageProgress": 33.0, "taskCount": 3, "completedCount": 0 }
]
```

**GET `/api/v1/dashboard/people-summary`**
```bash
curl -X GET http://localhost:8080/api/v1/dashboard/people-summary
```
*200 OK*
```json
[
  { "name": "Alice Smith", "role": "Backend engineer", "averageProgress": 62.5, "assignedCount": 4, "completedCount": 1 }
]
```

**GET `/api/v1/dashboard/search?q=`** — global search across people and tasks
```bash
curl -X GET "http://localhost:8080/api/v1/dashboard/search?q=alice"
```
*200 OK*
```json
{
  "people": [
    { "id": 2, "fullName": "Alice Smith", "email": "alice@example.com", "role": "Backend engineer", "teamName": "Platform", "teamId": 1 }
  ],
  "tasks": []
}
```

---

## Error Response Shape

All errors (400/404/409/500) share one envelope:
```json
{
  "timestamp": "2026-08-26T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Reassigning to the current owner is not allowed.",
  "path": "/api/v1/tasks/1/reassign"
}
```
`fieldErrors` is an extra key added only for `@Valid` request-body validation
failures — one entry per invalid field, e.g.:
```json
{
  "timestamp": "2026-08-26T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/tasks",
  "fieldErrors": { "title": "must not be blank" }
}
```
