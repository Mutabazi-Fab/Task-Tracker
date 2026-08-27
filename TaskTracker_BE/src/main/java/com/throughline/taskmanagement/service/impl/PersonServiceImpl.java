package com.throughline.taskmanagement.service.impl;

import com.throughline.taskmanagement.dto.request.CreatePersonRequest;
import com.throughline.taskmanagement.dto.response.PersonResponse;
import com.throughline.taskmanagement.dto.response.PersonStatisticsResponse;
import com.throughline.taskmanagement.dto.response.PersonTaskHistoryResponse;
import com.throughline.taskmanagement.dto.response.PersonTeamStatisticsResponse;
import com.throughline.taskmanagement.exception.DuplicateResourceException;
import com.throughline.taskmanagement.exception.ResourceNotFoundException;
import com.throughline.taskmanagement.mapper.PersonMapper;
import com.throughline.taskmanagement.model.Person;
import com.throughline.taskmanagement.model.Task;
import com.throughline.taskmanagement.model.Team;
import com.throughline.taskmanagement.model.TeamMember;
import com.throughline.taskmanagement.repository.PersonRepository;
import com.throughline.taskmanagement.repository.TaskCommentRepository;
import com.throughline.taskmanagement.repository.TaskRepository;
import com.throughline.taskmanagement.repository.TeamMemberRepository;
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
    private final PersonMapper personMapper;

    @Override
    public PersonResponse createPerson(CreatePersonRequest request) {
        if (personRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already exists: " + request.email());
        }

        Person person = new Person();
        person.setFullName(request.fullName());
        person.setEmail(request.email());
        person.setJobTitle(request.jobTitle());
        person.setRank(request.rank());

        Person saved = personRepository.save(person);
        return personMapper.toResponse(saved, List.of());
    }

    @Override
    public PersonResponse getPersonById(Long id) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Person not found"));
        return personMapper.toResponse(person, teamMemberRepository.findByPersonId(id));
    }

    @Override
    public Page<PersonResponse> getAllPeople(Pageable pageable) {
        return personRepository.findAll(pageable)
                .map(p -> personMapper.toResponse(p, teamMemberRepository.findByPersonId(p.getId())));
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
    public PersonStatisticsResponse getPersonStatistics(Long personId) {
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
    public Page<PersonTaskHistoryResponse> getPersonTaskHistory(Long personId, Pageable pageable) {
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
