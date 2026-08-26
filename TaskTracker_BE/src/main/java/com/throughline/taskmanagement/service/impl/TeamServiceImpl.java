package com.throughline.taskmanagement.service.impl;

import com.throughline.taskmanagement.dto.request.CreateTeamRequest;
import com.throughline.taskmanagement.dto.response.TaskListResponse;
import com.throughline.taskmanagement.dto.response.TeamResponse;
import com.throughline.taskmanagement.dto.response.TeamStatisticsResponse;
import com.throughline.taskmanagement.exception.DuplicateResourceException;
import com.throughline.taskmanagement.exception.InvalidAssignmentException;
import com.throughline.taskmanagement.exception.ResourceNotFoundException;
import com.throughline.taskmanagement.mapper.TaskMapper;
import com.throughline.taskmanagement.mapper.TeamMapper;
import com.throughline.taskmanagement.model.Person;
import com.throughline.taskmanagement.model.Task;
import com.throughline.taskmanagement.model.TaskComment;
import com.throughline.taskmanagement.model.Team;
import com.throughline.taskmanagement.repository.PersonRepository;
import com.throughline.taskmanagement.repository.TaskCommentRepository;
import com.throughline.taskmanagement.repository.TaskRepository;
import com.throughline.taskmanagement.repository.TeamRepository;
import com.throughline.taskmanagement.service.TeamService;
import lombok.RequiredArgsConstructor;
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
    private final TaskMapper taskMapper;
    private final TeamMapper teamMapper;

    @Override
    public TeamResponse createTeam(CreateTeamRequest request) {
        if (teamRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Team name already exists: " + request.name());
        }

        Team team = new Team();
        team.setName(request.name());

        if (request.teamLeaderId() != null) {
            Person leader = personRepository.findById(request.teamLeaderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Leader person not found"));
            team.setTeamLeader(leader);
            leader.setTeam(team);
        }

        Team saved = teamRepository.save(team);
        return teamMapper.toResponse(saved);
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
    public TeamResponse updateTeam(Long id, CreateTeamRequest request) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
        
        if (!team.getName().equals(request.name()) && teamRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Team name already exists: " + request.name());
        }
        
        team.setName(request.name());
        
        if (request.teamLeaderId() != null) {
            Person leader = personRepository.findById(request.teamLeaderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Leader person not found"));
            team.setTeamLeader(leader);
            leader.setTeam(team);
        } else {
            team.setTeamLeader(null);
        }
        
        return teamMapper.toResponse(teamRepository.save(team));
    }

    @Override
    public void deleteTeam(Long id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
        for (Person member : team.getMembers()) {
            member.setTeam(null);
        }
        teamRepository.delete(team);
    }

    @Override
    public TeamResponse setTeamLeader(Long teamId, Long personId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new ResourceNotFoundException("Person not found"));
                
        if (person.getTeam() == null || !person.getTeam().getId().equals(teamId)) {
            throw new InvalidAssignmentException("Leader must be a member of the team");
        }
        
        team.setTeamLeader(person);
        return teamMapper.toResponse(teamRepository.save(team));
    }

    @Override
    public TeamResponse addMember(Long teamId, Long personId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new ResourceNotFoundException("Person not found"));
                
        person.setTeam(team);
        team.getMembers().add(person);
        personRepository.save(person);
        
        return teamMapper.toResponse(teamRepository.save(team));
    }

    @Override
    public TeamResponse removeMember(Long teamId, Long personId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new ResourceNotFoundException("Person not found"));
                
        if (person.getTeam() != null && person.getTeam().getId().equals(teamId)) {
            person.setTeam(null);
            team.getMembers().remove(person);
            if (team.getTeamLeader() != null && team.getTeamLeader().getId().equals(personId)) {
                team.setTeamLeader(null);
            }
            personRepository.save(person);
        }
        
        return teamMapper.toResponse(teamRepository.save(team));
    }

    @Override
    public TeamStatisticsResponse getTeamStatistics(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
                
        Double teamAvgProgress = taskRepository.getAverageProgressByAssignedTeamId(teamId);
        long taskCount = taskRepository.findByAssignedTeamId(teamId).size();
        long completedCount = taskRepository.findByAssignedTeamId(teamId).stream()
                .filter(t -> t.getProgressPercentage() == 100).count();
                
        List<TeamStatisticsResponse.MemberProgress> memberProgresses = team.getMembers().stream()
                .map(m -> new TeamStatisticsResponse.MemberProgress(
                        m.getFullName(),
                        taskRepository.getAverageProgressByAssignedPersonId(m.getId())
                ))
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
}
