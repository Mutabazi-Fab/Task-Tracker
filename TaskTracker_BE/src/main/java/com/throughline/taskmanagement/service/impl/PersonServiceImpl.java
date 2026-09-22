package com.throughline.taskmanagement.service.impl;

import com.throughline.taskmanagement.dto.request.AddDailyGoalRequest;
import com.throughline.taskmanagement.dto.request.ChangeRoleRequest;
import com.throughline.taskmanagement.dto.request.CreatePersonRequest;
import com.throughline.taskmanagement.dto.request.SendPasswordResetRequest;
import com.throughline.taskmanagement.dto.request.SetAccountActiveRequest;
import com.throughline.taskmanagement.dto.response.PersonResponse;
import com.throughline.taskmanagement.dto.response.PersonStatisticsResponse;
import com.throughline.taskmanagement.dto.response.PersonTaskHistoryResponse;
import com.throughline.taskmanagement.dto.response.AccountStatusChangeResponse;
import com.throughline.taskmanagement.dto.response.PersonTeamStatisticsResponse;
import com.throughline.taskmanagement.dto.response.RoleChangeResponse;
import com.throughline.taskmanagement.dto.response.TaskListResponse;
import com.throughline.taskmanagement.enums.Role;
import com.throughline.taskmanagement.exception.DuplicateResourceException;
import com.throughline.taskmanagement.exception.ForbiddenActionException;
import com.throughline.taskmanagement.exception.InvalidAssignmentException;
import com.throughline.taskmanagement.exception.ResourceNotFoundException;
import com.throughline.taskmanagement.mapper.PersonMapper;
import com.throughline.taskmanagement.mapper.TaskMapper;
import com.throughline.taskmanagement.model.AccountStatusChange;
import com.throughline.taskmanagement.model.Department;
import com.throughline.taskmanagement.model.Person;
import com.throughline.taskmanagement.model.PersonDailyGoal;
import com.throughline.taskmanagement.model.RoleChange;
import com.throughline.taskmanagement.model.Task;
import com.throughline.taskmanagement.model.Team;
import com.throughline.taskmanagement.model.TeamMember;
import com.throughline.taskmanagement.repository.AccountStatusChangeRepository;
import com.throughline.taskmanagement.repository.DepartmentRepository;
import com.throughline.taskmanagement.repository.PersonDailyGoalRepository;
import com.throughline.taskmanagement.repository.PersonRepository;
import com.throughline.taskmanagement.repository.RoleChangeRepository;
import com.throughline.taskmanagement.repository.TaskCommentRepository;
import com.throughline.taskmanagement.repository.TaskRepository;
import com.throughline.taskmanagement.repository.TeamMemberRepository;
import com.throughline.taskmanagement.service.AuthService;
import com.throughline.taskmanagement.service.NotificationService;
import com.throughline.taskmanagement.service.PersonService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class PersonServiceImpl implements PersonService {

    private final PersonRepository personRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TaskRepository taskRepository;
    private final TaskCommentRepository taskCommentRepository;
    private final RoleChangeRepository roleChangeRepository;
    private final AccountStatusChangeRepository accountStatusChangeRepository;
    private final DepartmentRepository departmentRepository;
    private final PersonDailyGoalRepository personDailyGoalRepository;
    private final PersonMapper personMapper;
    private final TaskMapper taskMapper;
    private final NotificationService notificationService;
    private final AuthService authService;

    @Override
    public PersonResponse createPerson(CreatePersonRequest request) {
        if (personRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already exists: " + request.email());
        }
        if (request.createdById() == null) {
            throw new InvalidAssignmentException("createdById is required.");
        }

        Person createdBy = personRepository.findById(request.createdById())
                .orElseThrow(() -> new ResourceNotFoundException("createdById not found"));
        // There is no public self-registration — a Super Admin is the only one who can
        // create a login-enabled account, full stop, not just for a non-Member role.
        requireSuperAdmin(createdBy, "Only a Super Admin can create a new account.");

        Role targetRole = request.role() != null ? request.role() : Role.MEMBER;

        if (request.departmentId() == null) {
            throw new InvalidAssignmentException("departmentId is required.");
        }
        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new ResourceNotFoundException("departmentId not found"));

        Person person = new Person();
        person.setFullName(request.fullName());
        person.setEmail(request.email());
        person.setJobTitle(request.jobTitle());
        person.setRank(request.rank());
        person.setRole(targetRole);
        person.setDepartment(department);
        // No password yet, and not verified — the Super Admin vouches for who this person
        // is, not for a password. AuthService.sendSignUpCode below emails them a code to
        // set their own password (see AuthService.signUp), the same way a fresh account
        // always has going forward.

        Person saved = personRepository.save(person);

        // Best-effort — a flaky mail send shouldn't block onboarding; the Super Admin can
        // always trigger a resend later (see AuthController's resend-otp).
        try {
            authService.sendSignUpCode(saved);
        } catch (Exception e) {
            // Ignored on purpose — see comment above.
        }

        return personMapper.toResponse(saved, List.of());
    }

    @Override
    public PersonResponse changeRole(Long personId, ChangeRoleRequest request) {
        Person changedBy = personRepository.findById(request.changedById())
                .orElseThrow(() -> new ResourceNotFoundException("changedById not found"));
        requireSuperAdmin(changedBy, "Only a Super Admin can change someone's role.");

        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new ResourceNotFoundException("Person not found"));

        Role oldRole = person.getRole();
        Role newRole = request.newRole();

        if (oldRole == Role.SUPER_ADMIN && newRole != Role.SUPER_ADMIN
                && personRepository.countByRoleAndActiveTrue(Role.SUPER_ADMIN) <= 1) {
            throw new InvalidAssignmentException("Cannot remove the last Super Admin.");
        }

        RoleChange change = new RoleChange();
        change.setPerson(person);
        change.setOldRole(oldRole);
        change.setNewRole(newRole);
        change.setChangedBy(changedBy);
        change.setReason(request.reason());
        roleChangeRepository.save(change);

        person.setRole(newRole);
        Person saved = personRepository.save(person);

        notificationService.notifyRoleChange(saved, oldRole, newRole, changedBy);

        return personMapper.toResponse(saved, teamMemberRepository.findByPersonId(personId));
    }

    @Override
    public PersonResponse setActive(Long personId, SetAccountActiveRequest request) {
        Person changedBy = personRepository.findById(request.changedById())
                .orElseThrow(() -> new ResourceNotFoundException("changedById not found"));
        requireSuperAdmin(changedBy, "Only a Super Admin can activate or deactivate an account.");

        if (changedBy.getId().equals(personId) && !request.active()) {
            throw new ForbiddenActionException("You cannot deactivate your own account.");
        }

        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new ResourceNotFoundException("Person not found"));

        if (!request.active() && person.getRole() == Role.SUPER_ADMIN
                && personRepository.countByRoleAndActiveTrue(Role.SUPER_ADMIN) <= 1) {
            throw new InvalidAssignmentException("Cannot deactivate the last Super Admin.");
        }

        person.setActive(request.active());
        Person saved = personRepository.save(person);

        AccountStatusChange change = new AccountStatusChange();
        change.setPerson(saved);
        change.setActive(request.active());
        change.setChangedBy(changedBy);
        change.setReason(request.reason());
        accountStatusChangeRepository.save(change);

        notificationService.notifyAccountStatusChange(saved, request.active(), changedBy, request.reason());

        return personMapper.toResponse(saved, teamMemberRepository.findByPersonId(personId));
    }

    @Override
    public Page<RoleChangeResponse> getRoleChangeActivity(Long requesterId, Pageable pageable) {
        Person requester = personRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Person not found"));
        // Viewing this log is Director-or-above, same tier as Task Activity — a level
        // below requireSuperAdmin, which stays reserved for actually MAKING a role change.
        if (!Role.isAtLeastDirector(requester.getRole())) {
            throw new ForbiddenActionException("Only a Director or Super Admin can view role-change activity.");
        }

        return roleChangeRepository.findAllByOrderByTimestampDesc(pageable).map(this::toRoleChangeResponse);
    }

    @Override
    public Page<AccountStatusChangeResponse> getAccountStatusChangeActivity(Long requesterId, Pageable pageable) {
        Person requester = personRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Person not found"));
        // Same reasoning as getRoleChangeActivity above — viewing is Director-or-above,
        // actually (de)activating an account stays Super-Admin-only (setPersonActive).
        if (!Role.isAtLeastDirector(requester.getRole())) {
            throw new ForbiddenActionException("Only a Director or Super Admin can view account-status activity.");
        }

        return accountStatusChangeRepository.findAllByOrderByTimestampDesc(pageable)
                .map(this::toAccountStatusChangeResponse);
    }

    @Override
    public void sendPasswordReset(Long personId, SendPasswordResetRequest request) {
        Person changedBy = personRepository.findById(request.changedById())
                .orElseThrow(() -> new ResourceNotFoundException("changedById not found"));
        requireSuperAdmin(changedBy, "Only a Super Admin can send someone a password reset.");

        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new ResourceNotFoundException("Person not found"));
        if (person.getPassword() == null) {
            throw new InvalidAssignmentException("This person hasn't signed up yet, so there's no password to reset.");
        }

        authService.sendPasswordResetCode(person);
        notificationService.notifyPasswordResetRequested(person, changedBy);
    }

    private RoleChangeResponse toRoleChangeResponse(RoleChange c) {
        return new RoleChangeResponse(
                c.getId(),
                c.getPerson().getId(),
                c.getPerson().getFullName(),
                c.getOldRole(),
                c.getNewRole(),
                c.getChangedBy().getFullName(),
                c.getReason(),
                c.getTimestamp()
        );
    }

    private AccountStatusChangeResponse toAccountStatusChangeResponse(AccountStatusChange c) {
        return new AccountStatusChangeResponse(
                c.getId(),
                c.getPerson().getId(),
                c.getPerson().getFullName(),
                c.isActive(),
                c.getChangedBy().getFullName(),
                c.getReason(),
                c.getTimestamp()
        );
    }

    private void requireSuperAdmin(Person person, String message) {
        if (person.getRole() != Role.SUPER_ADMIN) {
            throw new ForbiddenActionException(message);
        }
    }

    /** A Director/Super Admin can view anyone's profile/stats/task-history. Anyone else
     *  can only view their own, or a teammate's (someone who shares at least one team
     *  with them) — everyone else is forbidden, not just hidden by the frontend. */
    private void requireCanViewPerson(Long viewerId, Long targetId) {
        if (viewerId.equals(targetId)) {
            return;
        }
        Person viewer = personRepository.findById(viewerId)
                .orElseThrow(() -> new ResourceNotFoundException("Person not found"));
        if (Role.isAtLeastDirector(viewer.getRole())) {
            return;
        }
        if (!teamMemberRepository.existsSharedTeam(viewerId, targetId)) {
            throw new ForbiddenActionException("You can only view your own profile or a teammate's.");
        }
    }

    @Override
    public PersonResponse getPersonById(Long id, Long viewerId) {
        requireCanViewPerson(viewerId, id);
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Person not found"));
        return personMapper.toResponse(person, teamMemberRepository.findByPersonId(id));
    }

    @Override
    public Page<PersonResponse> getAllPeople(Long viewerId, Pageable pageable) {
        Person viewer = personRepository.findById(viewerId)
                .orElseThrow(() -> new ResourceNotFoundException("Person not found"));

        Page<Person> people = Role.isAtLeastDirector(viewer.getRole())
                ? personRepository.findAll(pageable)
                : personRepository.findTeammatesOf(viewerId, pageable);

        return people.map(p -> personMapper.toResponse(p, teamMemberRepository.findByPersonId(p.getId())));
    }

    @Override
    public PersonResponse updatePerson(Long id, CreatePersonRequest request) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Person not found"));

        if (!person.getEmail().equals(request.email()) && personRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already exists: " + request.email());
        }

        person.setFullName(request.fullName());
        person.setEmail(request.email());
        person.setJobTitle(request.jobTitle());
        person.setRank(request.rank());

        Person saved = personRepository.save(person);
        return personMapper.toResponse(saved, teamMemberRepository.findByPersonId(id));
    }

    @Override
    public void deletePerson(Long id) {
        if (!personRepository.existsById(id)) {
            throw new ResourceNotFoundException("Person not found");
        }
        personRepository.deleteById(id);
    }

    @Override
    public PersonStatisticsResponse getPersonStatistics(Long personId, Long viewerId) {
        requireCanViewPerson(viewerId, personId);
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new ResourceNotFoundException("Person not found"));

        List<Task> tasks = taskRepository.findByAssignedPersonId(personId);

        long tasksAssigned = tasks.size();
        long tasksCompleted = tasks.stream().filter(t -> t.getProgressPercentage() == 100).count();
        long tasksOngoing = tasks.stream().filter(t -> t.getProgressPercentage() > 0 && t.getProgressPercentage() < 100).count();
        long tasksPending = tasks.stream().filter(t -> t.getProgressPercentage() == 0).count();

        Double avgProgress = taskRepository.getAverageProgressByAssignedPersonId(personId);
        long commentsLogged = taskCommentRepository.countByAuthorId(personId);

        List<Task> connected = taskRepository.findTasksConnectedToPerson(personId);
        long tasksHandedOff = connected.stream()
                .filter(t -> t.getReassignments().stream()
                        .anyMatch(r -> r.getFromPerson() != null && r.getFromPerson().getId().equals(personId))
                        && (t.getAssignedPerson() == null || !t.getAssignedPerson().getId().equals(personId)))
                .count();

        boolean fullyCompleted = tasksAssigned > 0 && tasksCompleted == tasksAssigned;

        List<PersonDailyGoal> goals = personDailyGoalRepository.findByPersonIdOrderByAddedAtAsc(personId);
        List<TaskListResponse> dailyGoalTasks = goals.stream()
                .map(goal -> taskMapper.toListResponse(goal.getTask(),
                        taskCommentRepository.findFirstByTaskIdOrderByCreatedAtDesc(goal.getTask().getId()).orElse(null)))
                .toList();

        return new PersonStatisticsResponse(
                avgProgress,
                tasksAssigned,
                tasksCompleted,
                tasksOngoing,
                tasksPending,
                commentsLogged,
                tasksHandedOff,
                fullyCompleted,
                getPersonTeamBreakdown(personId),
                dailyGoalTasks
        );
    }

    @Override
    public PersonStatisticsResponse addDailyGoal(Long personId, Long taskId, Long actorId) {
        requireSelf(actorId, personId, "You can only manage your own daily goals.");

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        if (task.getAssignedPerson() == null || !task.getAssignedPerson().getId().equals(personId)) {
            throw new InvalidAssignmentException("You can only pick from your own assigned tasks.");
        }
        if (personDailyGoalRepository.findByPersonIdAndTaskId(personId, taskId).isPresent()) {
            throw new InvalidAssignmentException("That task is already one of your daily goals.");
        }
        if (personDailyGoalRepository.countByPersonId(personId) >= 3) {
            throw new InvalidAssignmentException("You can only focus on up to 3 tasks at a time — remove one first.");
        }

        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new ResourceNotFoundException("Person not found"));

        PersonDailyGoal goal = new PersonDailyGoal();
        goal.setPerson(person);
        goal.setTask(task);
        personDailyGoalRepository.save(goal);

        return getPersonStatistics(personId, personId);
    }

    @Override
    public PersonStatisticsResponse removeDailyGoal(Long personId, Long taskId, Long actorId) {
        requireSelf(actorId, personId, "You can only manage your own daily goals.");

        // Removing something already gone isn't an error — same "silently fine" shape as
        // other idempotent removal endpoints in this codebase.
        personDailyGoalRepository.findByPersonIdAndTaskId(personId, taskId)
                .ifPresent(personDailyGoalRepository::delete);

        return getPersonStatistics(personId, personId);
    }

    private void requireSelf(Long actorId, Long personId, String message) {
        if (!actorId.equals(personId)) {
            throw new ForbiddenActionException(message);
        }
    }

    @Override
    public List<PersonTeamStatisticsResponse> getPersonTeamBreakdown(Long personId) {
        List<TeamMember> memberships = teamMemberRepository.findByPersonId(personId);

        return memberships.stream().map(m -> {
            Team team = m.getTeam();
            List<Task> teamTasks = taskRepository.findByAssignedPersonIdAndTeamId(personId, team.getId());

            long assigned = teamTasks.size();
            long completed = teamTasks.stream().filter(t -> t.getProgressPercentage() == 100).count();
            Double avg = teamTasks.stream().mapToInt(Task::getProgressPercentage).average().orElse(0.0);

            return new PersonTeamStatisticsResponse(team.getId(), team.getName(), avg, assigned, completed);
        }).toList();
    }

    @Override
    public Page<PersonTaskHistoryResponse> getPersonTaskHistory(Long personId, Long viewerId, Pageable pageable) {
        requireCanViewPerson(viewerId, personId);
        if (!personRepository.existsById(personId)) {
            throw new ResourceNotFoundException("Person not found");
        }

        return taskRepository.findTasksConnectedToPerson(personId, pageable)
                .map(t -> new PersonTaskHistoryResponse(t.getId(), t.getTaskCode(), t.getTitle(), involvementLabelFor(t, personId)));
    }

    private String involvementLabelFor(Task t, Long personId) {
        if (t.getAssignedPerson() != null && t.getAssignedPerson().getId().equals(personId)) {
            return "CURRENT_OWNER";
        }
        if (t.getAssignedTeam() != null && teamMemberRepository.existsByTeamIdAndPersonId(t.getAssignedTeam().getId(), personId)) {
            return "VIA_TEAM";
        }
        if (t.getReassignments().stream().anyMatch(r -> r.getFromPerson() != null && r.getFromPerson().getId().equals(personId))) {
            return "PREVIOUSLY_ASSIGNED";
        }
        if (t.getComments().stream().anyMatch(c -> c.getAuthor().getId().equals(personId))) {
            return "COMMENTER_ONLY";
        }
        return "UNKNOWN";
    }
}
