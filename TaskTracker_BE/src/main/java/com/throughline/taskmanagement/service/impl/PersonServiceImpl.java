package com.throughline.taskmanagement.service.impl;

import com.throughline.taskmanagement.dto.request.CreatePersonRequest;
import com.throughline.taskmanagement.dto.response.PersonResponse;
import com.throughline.taskmanagement.dto.response.PersonStatisticsResponse;
import com.throughline.taskmanagement.dto.response.PersonTaskHistoryResponse;
import com.throughline.taskmanagement.exception.DuplicateResourceException;
import com.throughline.taskmanagement.exception.ResourceNotFoundException;
import com.throughline.taskmanagement.mapper.PersonMapper;
import com.throughline.taskmanagement.model.Person;
import com.throughline.taskmanagement.model.Task;
import com.throughline.taskmanagement.model.Team;
import com.throughline.taskmanagement.repository.PersonRepository;
import com.throughline.taskmanagement.repository.TaskCommentRepository;
import com.throughline.taskmanagement.repository.TaskRepository;
import com.throughline.taskmanagement.repository.TeamRepository;
import com.throughline.taskmanagement.service.PersonService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class PersonServiceImpl implements PersonService {

    private final PersonRepository personRepository;
    private final TeamRepository teamRepository;
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
        person.setRole(request.role());
        person.setRank(request.rank());

        if (request.teamId() != null) {
            Team team = teamRepository.findById(request.teamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
            person.setTeam(team);
        }

        return personMapper.toResponse(personRepository.save(person));
    }

    @Override
    public PersonResponse getPersonById(Long id) {
        return personMapper.toResponse(personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Person not found")));
    }

    @Override
    public List<PersonResponse> getAllPeople() {
        return personRepository.findAll().stream().map(personMapper::toResponse).toList();
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
        person.setRole(request.role());
        person.setRank(request.rank());

        if (request.teamId() != null) {
            Team team = teamRepository.findById(request.teamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
            person.setTeam(team);
        } else {
            person.setTeam(null);
        }

        return personMapper.toResponse(personRepository.save(person));
    }

    @Override
    public void deletePerson(Long id) {
        if (!personRepository.existsById(id)) {
            throw new ResourceNotFoundException("Person not found");
        }
        personRepository.deleteById(id);
    }

    @Override
    public PersonResponse assignToTeam(Long personId, Long teamId) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new ResourceNotFoundException("Person not found"));
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));

        person.setTeam(team);
        return personMapper.toResponse(personRepository.save(person));
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
                fullyCompleted
        );
    }

    @Override
    public List<PersonTaskHistoryResponse> getPersonTaskHistory(Long personId) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new ResourceNotFoundException("Person not found"));

        List<Task> tasks = taskRepository.findTasksConnectedToPerson(personId);
        List<PersonTaskHistoryResponse> result = new ArrayList<>();

        for (Task t : tasks) {
            String label = "UNKNOWN";
            if (t.getAssignedPerson() != null && t.getAssignedPerson().getId().equals(personId)) {
                label = "CURRENT_OWNER";
            } else if (t.getAssignedTeam() != null && person.getTeam() != null && t.getAssignedTeam().getId().equals(person.getTeam().getId())) {
                label = "VIA_TEAM";
            } else if (t.getReassignments().stream().anyMatch(r -> r.getFromPerson() != null && r.getFromPerson().getId().equals(personId))) {
                label = "PREVIOUSLY_ASSIGNED";
            } else if (t.getComments().stream().anyMatch(c -> c.getAuthor().getId().equals(personId))) {
                label = "COMMENTER_ONLY";
            }
            
            result.add(new PersonTaskHistoryResponse(t.getId(), t.getTaskCode(), t.getTitle(), label));
        }

        return result;
    }
}
