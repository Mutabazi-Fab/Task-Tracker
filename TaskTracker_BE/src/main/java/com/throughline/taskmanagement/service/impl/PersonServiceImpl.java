package com.throughline.taskmanagement.service.impl;

import com.throughline.taskmanagement.dto.request.ChangeRoleRequest;
import com.throughline.taskmanagement.dto.request.CreatePersonRequest;
import com.throughline.taskmanagement.dto.request.SendPasswordResetRequest;
import com.throughline.taskmanagement.dto.request.SetAccountActiveRequest;
import com.throughline.taskmanagement.dto.response.PersonResponse;
import com.throughline.taskmanagement.dto.response.PersonStatisticsResponse;
import com.throughline.taskmanagement.dto.response.PersonTaskHistoryResponse;
import com.throughline.taskmanagement.dto.response.PersonTeamStatisticsResponse;
import com.throughline.taskmanagement.dto.response.RoleChangeResponse;
import com.throughline.taskmanagement.enums.Role;
import com.throughline.taskmanagement.exception.DuplicateResourceException;
import com.throughline.taskmanagement.exception.ForbiddenActionException;
import com.throughline.taskmanagement.exception.InvalidAssignmentException;
import com.throughline.taskmanagement.exception.ResourceNotFoundException;
import com.throughline.taskmanagement.mapper.PersonMapper;
import com.throughline.taskmanagement.model.Person;
import com.throughline.taskmanagement.model.RoleChange;
import com.throughline.taskmanagement.model.Task;
import com.throughline.taskmanagement.model.Team;
import com.throughline.taskmanagement.model.TeamMember;
import com.throughline.taskmanagement.repository.PersonRepository;
import com.throughline.taskmanagement.repository.RoleChangeRepository;
import com.throughline.taskmanagement.repository.TaskCommentRepository;
import com.throughline.taskmanagement.repository.TaskRepository;
import com.throughline.taskmanagement.repository.TeamMemberRepository;
import com.throughline.taskmanagement.service.AuthService;
import com.throughline.taskmanagement.service.MailService;
import com.throughline.taskmanagement.service.NotificationService;
import com.throughline.taskmanagement.service.PersonService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
    private final PersonMapper personMapper;
    private final NotificationService notificationService;
    private final MailService mailService;
    private final AuthService authService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

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
        requireDirector(createdBy, "Only a Director or Super Admin can add a new person.");

        Role targetRole = request.role() != null ? request.role() : Role.MEMBER;
        if (targetRole != Role.MEMBER) {
            requireSuperAdmin(createdBy, "Only a Super Admin can set a new person's role to Director or Super Admin.");
        }

        Person person = new Person();
        person.setFullName(request.fullName());
        person.setEmail(request.email());
        person.setJobTitle(request.jobTitle());
        person.setRank(request.rank());
        person.setRole(targetRole);
        // emailVerified defaults false (see Person.emailVerified) — new to the system,
        // still has to prove they control this inbox once they sign up to activate login.

        Person saved = personRepository.save(person);

        // Best-effort — the person record is created either way; a flaky mail send
        // shouldn't block whoever's onboarding them from doing so, they can always be
        // told directly instead.
        try {
            String roleWord = targetRole == Role.MEMBER ? "a team member" : "a " + targetRole.name().toLowerCase();
            mailService.send(
                    saved.getEmail(),
                    "You've been added to Throughline",
                    String.format(
                            "%s added you to Throughline as %s. Sign up at %s/signup using this email address (%s) to activate your account.",
                            createdBy.getFullName(), roleWord, frontendUrl, saved.getEmail()));
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

        notificationService.notifyAccountStatusChange(saved, request.active(), changedBy);

        return personMapper.toResponse(saved, teamMemberRepository.findByPersonId(personId));
    }

    @Override
    public Page<RoleChangeResponse> getRoleChangeActivity(Long requesterId, Pageable pageable) {
        Person requester = personRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Person not found"));
        requireSuperAdmin(requester, "Only a Super Admin can view role-change activity.");

        return roleChangeRepository.findAllByOrderByTimestampDesc(pageable).map(this::toRoleChangeResponse);
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

    private void requireDirector(Person person, String message) {
        if (!Role.isAtLeastDirector(person.getRole())) {
            throw new ForbiddenActionException(message);
        }
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

        return new PersonStatisticsResponse(
                avgProgress,
                tasksAssigned,
                tasksCompleted,
                tasksOngoing,
                tasksPending,
                commentsLogged,
                tasksHandedOff,
                fullyCompleted,
                getPersonTeamBreakdown(personId)
        );
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
