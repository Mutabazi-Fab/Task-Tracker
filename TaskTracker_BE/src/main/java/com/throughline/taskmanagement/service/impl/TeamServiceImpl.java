package com.throughline.taskmanagement.service.impl;

import com.throughline.taskmanagement.dto.request.AddTeamMemberRequest;
import com.throughline.taskmanagement.dto.request.CreateTeamRequest;
import com.throughline.taskmanagement.dto.request.RemoveTeamMemberRequest;
import com.throughline.taskmanagement.dto.request.SetTeamLeaderRequest;
import com.throughline.taskmanagement.dto.request.UpdateTeamRequest;
import com.throughline.taskmanagement.dto.response.TaskListResponse;
import com.throughline.taskmanagement.dto.response.TeamMemberResponse;
import com.throughline.taskmanagement.dto.response.TeamMembershipChangeResponse;
import com.throughline.taskmanagement.dto.response.TeamResponse;
import com.throughline.taskmanagement.dto.response.TeamStatisticsResponse;
import com.throughline.taskmanagement.enums.Role;
import com.throughline.taskmanagement.enums.TeamMembershipChangeAction;
import com.throughline.taskmanagement.exception.DuplicateResourceException;
import com.throughline.taskmanagement.exception.ForbiddenActionException;
import com.throughline.taskmanagement.exception.InvalidAssignmentException;
import com.throughline.taskmanagement.exception.ResourceNotFoundException;
import com.throughline.taskmanagement.mapper.TaskMapper;
import com.throughline.taskmanagement.mapper.TeamMapper;
import com.throughline.taskmanagement.model.Person;
import com.throughline.taskmanagement.model.Task;
import com.throughline.taskmanagement.model.TaskComment;
import com.throughline.taskmanagement.model.Team;
import com.throughline.taskmanagement.model.TeamMember;
import com.throughline.taskmanagement.model.TeamMembershipChange;
import com.throughline.taskmanagement.repository.PersonRepository;
import com.throughline.taskmanagement.repository.TaskCommentRepository;
import com.throughline.taskmanagement.repository.TaskRepository;
import com.throughline.taskmanagement.repository.TeamMemberRepository;
import com.throughline.taskmanagement.repository.TeamMembershipChangeRepository;
import com.throughline.taskmanagement.repository.TeamRepository;
import com.throughline.taskmanagement.service.NotificationService;
import com.throughline.taskmanagement.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class TeamServiceImpl implements TeamService {

    private final TeamRepository teamRepository;
    private final PersonRepository personRepository;
    private final TaskRepository taskRepository;
    private final TaskCommentRepository taskCommentRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamMembershipChangeRepository teamMembershipChangeRepository;
    private final NotificationService notificationService;
    private final TaskMapper taskMapper;
    private final TeamMapper teamMapper;

    @Override
    public TeamResponse createTeam(CreateTeamRequest request) {
        if (teamRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Team name already exists: " + request.name());
        }

        Person createdBy = personRepository.findById(request.createdById())
                .orElseThrow(() -> new ResourceNotFoundException("createdById not found"));
        requireDirector(createdBy, "Only a Director can create a team.");

        if (!request.memberIds().contains(request.leaderId())) {
            throw new InvalidAssignmentException("leaderId must be one of memberIds.");
        }

        Team team = new Team();
        team.setName(request.name());
        team.setCreatedBy(createdBy);
        Team saved = teamRepository.save(team);

        for (Long memberId : request.memberIds()) {
            Person member = personRepository.findById(memberId)
                    .orElseThrow(() -> new ResourceNotFoundException("Person not found: " + memberId));
            TeamMember teamMember = new TeamMember();
            teamMember.setTeam(saved);
            teamMember.setPerson(member);
            teamMember.setLeader(memberId.equals(request.leaderId()));
            saved.getMembers().add(teamMember);
        }

        return teamMapper.toResponse(teamRepository.save(saved));
    }

    @Override
    public TeamResponse getTeamById(Long id) {
        return teamMapper.toResponse(teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found")));
    }

    @Override
    public List<TeamResponse> getAllTeams() {
        return teamRepository.findAll().stream().map(teamMapper::toResponse).toList();
    }

    @Override
    public TeamResponse updateTeam(Long id, UpdateTeamRequest request) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));

        if (!team.getName().equals(request.name()) && teamRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Team name already exists: " + request.name());
        }

        team.setName(request.name());
        return teamMapper.toResponse(teamRepository.save(team));
    }

    @Override
    public void deleteTeam(Long id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
        // TeamMember rows cascade-delete with the team (Team.members is CascadeType.ALL +
        // orphanRemoval). TeamMembershipChange rows do NOT — deleting a team with membership
        // history intentionally fails on the FK constraint rather than silently destroying
        // the audit log; that's a decision for later, not a side effect of this call.
        teamRepository.delete(team);
    }

    @Override
    public TeamResponse setTeamLeader(Long teamId, Long personId, SetTeamLeaderRequest request) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
        Person changedBy = personRepository.findById(request.changedById())
                .orElseThrow(() -> new ResourceNotFoundException("changedById not found"));
        requireDirector(changedBy, "Only a Director can reassign a team's leader.");

        TeamMember target = teamMemberRepository.findByTeamIdAndPersonId(teamId, personId)
                .orElseThrow(() -> new InvalidAssignmentException("Leader must already be a member of the team."));

        teamMemberRepository.findByTeamIdAndIsLeaderTrue(teamId)
                .ifPresent(current -> current.setLeader(false));
        target.setLeader(true);

        return teamMapper.toResponse(teamRepository.findById(teamId).orElseThrow());
    }

    @Override
    public List<TeamMemberResponse> getTeamMembers(Long teamId) {
        if (!teamRepository.existsById(teamId)) {
            throw new ResourceNotFoundException("Team not found");
        }
        return teamMemberRepository.findByTeamId(teamId).stream()
                .map(m -> new TeamMemberResponse(
                        m.getPerson().getId(),
                        m.getPerson().getFullName(),
                        m.getPerson().getJobTitle(),
                        m.getPerson().getRank(),
                        m.isLeader(),
                        m.getJoinedAt()))
                .toList();
    }

    @Override
    public TeamResponse addMember(Long teamId, AddTeamMemberRequest request) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
        Person person = personRepository.findById(request.personId())
                .orElseThrow(() -> new ResourceNotFoundException("Person not found"));
        Person changedBy = personRepository.findById(request.changedById())
                .orElseThrow(() -> new ResourceNotFoundException("changedById not found"));

        requireDirectorOrTeamLeader(teamId, changedBy);

        if (teamMemberRepository.existsByTeamIdAndPersonId(teamId, request.personId())) {
            throw new DuplicateResourceException("This person is already a member of this team.");
        }

        TeamMember teamMember = new TeamMember();
        teamMember.setTeam(team);
        teamMember.setPerson(person);
        teamMember.setLeader(false);
        teamMemberRepository.save(teamMember);

        // Also creates a Notification for the team's Director, unless they made this change themself.
        logMembershipChange(team, person, TeamMembershipChangeAction.ADDED, changedBy, request.reason());

        return teamMapper.toResponse(teamRepository.findById(teamId).orElseThrow());
    }

    @Override
    public TeamResponse removeMember(Long teamId, Long personId, RemoveTeamMemberRequest request) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
        TeamMember membership = teamMemberRepository.findByTeamIdAndPersonId(teamId, personId)
                .orElseThrow(() -> new ResourceNotFoundException("This person is not a member of this team."));
        Person changedBy = personRepository.findById(request.changedById())
                .orElseThrow(() -> new ResourceNotFoundException("changedById not found"));

        requireDirectorOrTeamLeader(teamId, changedBy);

        // TODO: block this removal if the member has unfinished subtasks assigned to them
        // within this team, per the "block removal" decision from Phase 2. This is now
        // actually implementable (Task hierarchy/subtasks exist as of Phase 3) but hasn't
        // been wired in yet — a real, tracked gap, not an oversight.

        Person person = membership.getPerson();
        team.getMembers().remove(membership);
        teamMemberRepository.delete(membership);

        // Also creates a Notification for the team's Director, unless they made this change themself.
        logMembershipChange(team, person, TeamMembershipChangeAction.REMOVED, changedBy, request.reason());

        return teamMapper.toResponse(teamRepository.findById(teamId).orElseThrow());
    }

    @Override
    public Page<TeamMembershipChangeResponse> getMembershipHistory(Long teamId, Pageable pageable) {
        if (!teamRepository.existsById(teamId)) {
            throw new ResourceNotFoundException("Team not found");
        }
        return teamMembershipChangeRepository.findByTeamIdOrderByTimestampDesc(teamId, pageable)
                .map(this::toChangeResponse);
    }

    @Override
    public Page<TeamMembershipChangeResponse> getAllMembershipActivity(Pageable pageable) {
        return teamMembershipChangeRepository.findAllByOrderByTimestampDesc(pageable)
                .map(this::toChangeResponse);
    }

    @Override
    public TeamStatisticsResponse getTeamStatistics(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));

        Double teamAvgProgress = taskRepository.getAverageProgressByAssignedTeamId(teamId);
        long taskCount = taskRepository.findByAssignedTeamId(teamId).size();
        long completedCount = taskRepository.findByAssignedTeamId(teamId).stream()
                .filter(t -> t.getProgressPercentage() == 100).count();

        // Scoped to THIS team's tasks only — getAverageProgressByAssignedPersonId would wrongly
        // blend in a member's tasks from every other team they also belong to.
        List<TeamStatisticsResponse.MemberProgress> memberProgresses = team.getMembers().stream()
                .map(m -> {
                    List<Task> memberTeamTasks = taskRepository.findByAssignedPersonIdAndTeamId(m.getPerson().getId(), teamId);
                    Double avg = memberTeamTasks.stream().mapToInt(Task::getProgressPercentage).average().orElse(0.0);
                    return new TeamStatisticsResponse.MemberProgress(m.getPerson().getFullName(), avg);
                })
                .collect(Collectors.toList());

        return new TeamStatisticsResponse(
                teamAvgProgress,
                taskCount,
                team.getMembers().size(),
                completedCount,
                memberProgresses
        );
    }

    @Override
    public List<TaskListResponse> getTeamTasks(Long teamId) {
        if (!teamRepository.existsById(teamId)) {
            throw new ResourceNotFoundException("Team not found");
        }

        List<Task> tasks = taskRepository.findByAssignedTeamId(teamId);
        return tasks.stream().map(t -> {
            TaskComment lastComment = taskCommentRepository.findFirstByTaskIdOrderByCreatedAtDesc(t.getId()).orElse(null);
            return taskMapper.toListResponse(t, lastComment);
        }).collect(Collectors.toList());
    }

    private void requireDirector(Person person, String message) {
        if (person.getRole() != Role.DIRECTOR) {
            throw new ForbiddenActionException(message);
        }
    }

    private void requireDirectorOrTeamLeader(Long teamId, Person changedBy) {
        boolean isDirector = changedBy.getRole() == Role.DIRECTOR;
        boolean isThisTeamsLeader = teamMemberRepository.findByTeamIdAndPersonId(teamId, changedBy.getId())
                .map(TeamMember::isLeader)
                .orElse(false);

        if (!isDirector && !isThisTeamsLeader) {
            throw new ForbiddenActionException("Only this team's leader or a Director can change its membership.");
        }
    }

    private void logMembershipChange(Team team, Person person, TeamMembershipChangeAction action, Person changedBy, String reason) {
        TeamMembershipChange change = new TeamMembershipChange();
        change.setTeam(team);
        change.setPerson(person);
        change.setAction(action);
        change.setChangedBy(changedBy);
        change.setReason(reason);
        TeamMembershipChange saved = teamMembershipChangeRepository.save(change);
        notificationService.notifyMembershipChange(saved);
    }

    private TeamMembershipChangeResponse toChangeResponse(TeamMembershipChange change) {
        return new TeamMembershipChangeResponse(
                change.getId(),
                change.getTeam().getId(),
                change.getTeam().getName(),
                change.getPerson().getId(),
                change.getPerson().getFullName(),
                change.getAction(),
                change.getChangedBy().getFullName(),
                change.getReason(),
                change.getTimestamp()
        );
    }
}
