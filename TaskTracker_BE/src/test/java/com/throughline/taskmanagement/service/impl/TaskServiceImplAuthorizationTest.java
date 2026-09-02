package com.throughline.taskmanagement.service.impl;

import com.throughline.taskmanagement.dto.request.ReassignTaskRequest;
import com.throughline.taskmanagement.enums.Role;
import com.throughline.taskmanagement.enums.TaskStatus;
import com.throughline.taskmanagement.exception.ForbiddenActionException;
import com.throughline.taskmanagement.mapper.TaskMapper;
import com.throughline.taskmanagement.model.Person;
import com.throughline.taskmanagement.model.Task;
import com.throughline.taskmanagement.model.Team;
import com.throughline.taskmanagement.model.TeamMember;
import com.throughline.taskmanagement.repository.PersonRepository;
import com.throughline.taskmanagement.repository.TaskCommentRepository;
import com.throughline.taskmanagement.repository.TaskReassignmentRepository;
import com.throughline.taskmanagement.repository.TaskRepository;
import com.throughline.taskmanagement.repository.TeamMemberRepository;
import com.throughline.taskmanagement.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the two authorization rules on TaskServiceImpl that used to have NO server-side
 * check at all (reassignTask) or were only gated by the frontend hiding a button
 * (deleteTask) — see the security-hardening pass this project went through. Both are
 * private checks (requireCanReassign, the role check inside deleteTask), so they're
 * exercised here through the public methods that call them, exactly as a real request
 * would.
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceImplAuthorizationTest {

    @Mock private TaskRepository taskRepository;
    @Mock private PersonRepository personRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private TaskCommentRepository taskCommentRepository;
    @Mock private TaskReassignmentRepository taskReassignmentRepository;
    @Mock private TaskMapper taskMapper;

    private TaskServiceImpl taskService;

    private Team owningTeam;
    private Team otherTeam;
    private Task topLevelTask;

    @BeforeEach
    void setUp() {
        taskService = new TaskServiceImpl(taskRepository, personRepository, teamRepository,
                teamMemberRepository, taskCommentRepository, taskReassignmentRepository, taskMapper);

        owningTeam = new Team();
        owningTeam.setId(5L);
        owningTeam.setName("Digital Banking");

        otherTeam = new Team();
        otherTeam.setId(6L);
        otherTeam.setName("Mobile Banking");

        topLevelTask = new Task();
        topLevelTask.setId(13L);
        topLevelTask.setParentTask(null);
        topLevelTask.setAssignedTeam(owningTeam);
        topLevelTask.setStatus(TaskStatus.ONGOING);
    }

    private Person personWithRole(long id, Role role) {
        Person person = new Person();
        person.setId(id);
        person.setRole(role);
        return person;
    }

    // ---- reassignTask ----

    @Test
    void reassignTask_directorMayReassignAnyTeamsTask() {
        Person director = personWithRole(1L, Role.DIRECTOR);
        when(taskRepository.findWithDetailsById(13L)).thenReturn(Optional.of(topLevelTask));
        when(personRepository.findById(1L)).thenReturn(Optional.of(director));
        when(teamRepository.findById(6L)).thenReturn(Optional.of(otherTeam));

        ReassignTaskRequest request = new ReassignTaskRequest(6L, null, 1L, "handing off to Mobile Banking");

        assertDoesNotThrow(() -> taskService.reassignTask(13L, request));
    }

    @Test
    void reassignTask_leaderOfTheOwningTeamMayReassignIt() {
        Person leader = personWithRole(10L, Role.MEMBER);
        TeamMember leadership = new TeamMember();
        leadership.setTeam(owningTeam);
        leadership.setPerson(leader);
        leadership.setLeader(true);

        when(taskRepository.findWithDetailsById(13L)).thenReturn(Optional.of(topLevelTask));
        when(personRepository.findById(10L)).thenReturn(Optional.of(leader));
        when(teamMemberRepository.findByTeamIdAndIsLeaderTrue(5L)).thenReturn(Optional.of(leadership));
        when(teamRepository.findById(6L)).thenReturn(Optional.of(otherTeam));

        ReassignTaskRequest request = new ReassignTaskRequest(6L, null, 10L, "handing off to Mobile Banking");

        assertDoesNotThrow(() -> taskService.reassignTask(13L, request));
    }

    @Test
    void reassignTask_plainMemberOfTheOwningTeamIsForbidden() {
        Person member = personWithRole(11L, Role.MEMBER);
        Person someoneElseIsLeader = personWithRole(99L, Role.MEMBER);
        TeamMember leadership = new TeamMember();
        leadership.setTeam(owningTeam);
        leadership.setPerson(someoneElseIsLeader);
        leadership.setLeader(true);

        when(taskRepository.findWithDetailsById(13L)).thenReturn(Optional.of(topLevelTask));
        when(personRepository.findById(11L)).thenReturn(Optional.of(member));
        when(teamMemberRepository.findByTeamIdAndIsLeaderTrue(5L)).thenReturn(Optional.of(leadership));

        ReassignTaskRequest request = new ReassignTaskRequest(6L, null, 11L, "trying to hand this off");

        assertThrows(ForbiddenActionException.class, () -> taskService.reassignTask(13L, request));
        // Never even got to looking up the destination team — rejected purely on authority.
        verify(teamRepository, never()).findById(anyLong());
    }

    @Test
    void reassignTask_memberOfAnUnrelatedTeamIsForbidden() {
        Person unrelatedMember = personWithRole(12L, Role.MEMBER);

        when(taskRepository.findWithDetailsById(13L)).thenReturn(Optional.of(topLevelTask));
        when(personRepository.findById(12L)).thenReturn(Optional.of(unrelatedMember));
        when(teamMemberRepository.findByTeamIdAndIsLeaderTrue(5L)).thenReturn(Optional.empty());

        ReassignTaskRequest request = new ReassignTaskRequest(6L, null, 12L, "trying to hand this off");

        assertThrows(ForbiddenActionException.class, () -> taskService.reassignTask(13L, request));
    }

    // ---- deleteTask ----

    @Test
    void deleteTask_directorMayDelete() {
        Person director = personWithRole(1L, Role.DIRECTOR);
        when(personRepository.findById(1L)).thenReturn(Optional.of(director));
        when(taskRepository.findWithDetailsById(13L)).thenReturn(Optional.of(topLevelTask));

        assertDoesNotThrow(() -> taskService.deleteTask(13L, 1L));

        verify(taskRepository).delete(topLevelTask);
    }

    @Test
    void deleteTask_superAdminMayDelete() {
        Person superAdmin = personWithRole(19L, Role.SUPER_ADMIN);
        when(personRepository.findById(19L)).thenReturn(Optional.of(superAdmin));
        when(taskRepository.findWithDetailsById(13L)).thenReturn(Optional.of(topLevelTask));

        assertDoesNotThrow(() -> taskService.deleteTask(13L, 19L));

        verify(taskRepository).delete(topLevelTask);
    }

    @Test
    void deleteTask_plainMemberIsForbidden() {
        Person member = personWithRole(4L, Role.MEMBER);
        when(personRepository.findById(4L)).thenReturn(Optional.of(member));

        assertThrows(ForbiddenActionException.class, () -> taskService.deleteTask(13L, 4L));

        // Rejected before the task was even looked up, let alone deleted.
        verify(taskRepository, never()).findWithDetailsById(anyLong());
        verify(taskRepository, never()).delete(any());
    }

    @Test
    void deleteTask_teamLeaderIsStillForbidden() {
        // Unlike reassign, deleting is Director/Super-Admin-only — a Team Leader doesn't
        // get an exception here, even for their own team's task.
        Person leader = personWithRole(10L, Role.MEMBER);
        when(personRepository.findById(10L)).thenReturn(Optional.of(leader));

        assertThrows(ForbiddenActionException.class, () -> taskService.deleteTask(13L, 10L));
    }
}
