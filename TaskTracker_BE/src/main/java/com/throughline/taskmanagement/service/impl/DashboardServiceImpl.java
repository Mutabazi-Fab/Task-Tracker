package com.throughline.taskmanagement.service.impl;

import com.throughline.taskmanagement.dto.response.*;
import com.throughline.taskmanagement.enums.Role;
import com.throughline.taskmanagement.enums.TaskStatus;
import com.throughline.taskmanagement.exception.ForbiddenActionException;
import com.throughline.taskmanagement.exception.ResourceNotFoundException;
import com.throughline.taskmanagement.mapper.PersonMapper;
import com.throughline.taskmanagement.mapper.TaskMapper;
import com.throughline.taskmanagement.model.Person;
import com.throughline.taskmanagement.model.Task;
import com.throughline.taskmanagement.model.TaskComment;
import com.throughline.taskmanagement.model.Team;
import com.throughline.taskmanagement.repository.PersonRepository;
import com.throughline.taskmanagement.repository.TaskCommentRepository;
import com.throughline.taskmanagement.repository.TaskRepository;
import com.throughline.taskmanagement.repository.TeamMemberRepository;
import com.throughline.taskmanagement.repository.TeamRepository;
import com.throughline.taskmanagement.service.DashboardService;
import com.throughline.taskmanagement.service.PersonService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final TaskRepository taskRepository;
    private final TeamRepository teamRepository;
    private final PersonRepository personRepository;
    private final TaskCommentRepository taskCommentRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final PersonMapper personMapper;
    private final TaskMapper taskMapper;
    private final PersonService personService;

    @Override
    public DashboardOverviewResponse getOverview() {
        List<Task> tasks = taskRepository.findAll();
        long totalTasks = tasks.size();
        long completedCount = tasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();
        long ongoingCount = tasks.stream().filter(t -> t.getStatus() == TaskStatus.ONGOING).count();
        long pendingCount = tasks.stream().filter(t -> t.getStatus() == TaskStatus.PENDING).count();

        Double orgAverageProgress = tasks.stream()
                .mapToInt(Task::getProgressPercentage)
                .average()
                .orElse(0.0);

        return new DashboardOverviewResponse(orgAverageProgress, totalTasks, completedCount, ongoingCount, pendingCount);
    }

    @Override
    public List<StatusMixResponse> getStatusMix() {
        List<Task> tasks = taskRepository.findAll();
        long total = tasks.size();
        if (total == 0) return List.of();

        return List.of(TaskStatus.values()).stream().map(status -> {
            long count = tasks.stream().filter(t -> t.getStatus() == status).count();
            return new StatusMixResponse(status.name(), count, (double) count / total * 100);
        }).collect(Collectors.toList());
    }

    @Override
    public List<ProgressPointResponse> getProgressOverTime(LocalDate from, LocalDate to) {
        List<Task> allTasks = taskRepository.findAll();
        List<ProgressPointResponse> result = new ArrayList<>();

        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            LocalDateTime endOfDay = date.atTime(23, 59, 59);
            
            double sum = 0;
            int count = 0;
            
            for (Task t : allTasks) {
                if (t.getCreatedAt().isAfter(endOfDay)) {
                    continue;
                }
                
                int percentage = t.getComments().stream()
                        .filter(c -> !c.getCreatedAt().isAfter(endOfDay))
                        .max(Comparator.comparing(TaskComment::getCreatedAt))
                        .map(TaskComment::getPercentageAtComment)
                        .orElse(0);
                
                sum += percentage;
                count++;
            }
            
            double averagePercentage = count == 0 ? 0 : sum / count;
            result.add(new ProgressPointResponse(date, averagePercentage));
        }
        
        return result;
    }

    @Override
    public List<TeamLeaderboardResponse> getTeamLeaderboard() {
        List<Team> teams = teamRepository.findAll();
        return teams.stream().map(team -> {
            List<Task> teamTasks = taskRepository.findByAssignedTeamId(team.getId());
            Double avgProgress = teamTasks.stream().mapToInt(Task::getProgressPercentage).average().orElse(0.0);
            long completed = teamTasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();
            
            String leaderName = teamMemberRepository.findByTeamIdAndIsLeaderTrue(team.getId())
                    .map(tm -> tm.getPerson().getFullName())
                    .orElse("No Leader");

            return new TeamLeaderboardResponse(
                    team.getName(),
                    leaderName,
                    avgProgress,
                    teamTasks.size(),
                    completed
            );
        })
        .sorted(Comparator.comparing(TeamLeaderboardResponse::averageProgress).reversed())
        .collect(Collectors.toList());
    }

    @Override
    public List<PersonSummaryResponse> getPeopleSummary() {
        List<Person> people = personRepository.findAll();
        return people.stream().map(person -> {
            List<Task> personTasks = taskRepository.findByAssignedPersonId(person.getId());
            Double avgProgress = personTasks.stream().mapToInt(Task::getProgressPercentage).average().orElse(0.0);
            long completed = personTasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();
            
            return new PersonSummaryResponse(
                    person.getFullName(),
                    person.getJobTitle(),
                    avgProgress,
                    personTasks.size(),
                    completed
            );
        }).collect(Collectors.toList());
    }

    @Override
    public GlobalSearchResponse globalSearch(String q) {
        // Per-team breakdown for each matched person — not one blended number across every
        // team they belong to (see PersonService.getPersonTeamBreakdown).
        List<PersonSearchResultResponse> people = personRepository.search(q).stream()
                .map(p -> new PersonSearchResultResponse(
                        personMapper.toResponse(p, teamMemberRepository.findByPersonId(p.getId())),
                        personService.getPersonTeamBreakdown(p.getId())
                ))
                .toList();
        List<TaskListResponse> tasks = taskRepository.search(q).stream().map(t -> {
            TaskComment lastComment = taskCommentRepository.findFirstByTaskIdOrderByCreatedAtDesc(t.getId()).orElse(null);
            return taskMapper.toListResponse(t, lastComment);
        }).toList();

        return new GlobalSearchResponse(people, tasks);
    }

    @Override
    public Page<TaskListResponse> getDirectorTasks(Long directorId, Pageable pageable) {
        Person director = personRepository.findById(directorId)
                .orElseThrow(() -> new ResourceNotFoundException("Person not found"));
        if (!Role.isAtLeastDirector(director.getRole())) {
            throw new ForbiddenActionException("Only a Director has a Director's Dashboard.");
        }

        return taskRepository.findByParentTaskIsNullAndAssignedById(directorId, pageable)
                .map(t -> {
                    TaskComment lastComment = taskCommentRepository.findFirstByTaskIdOrderByCreatedAtDesc(t.getId()).orElse(null);
                    return taskMapper.toListResponse(t, lastComment);
                });
    }
}
