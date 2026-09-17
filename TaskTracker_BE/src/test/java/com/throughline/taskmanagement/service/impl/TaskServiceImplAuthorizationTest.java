package com.throughline.taskmanagement.service.impl;

import com.throughline.taskmanagement.dto.request.ReassignTaskRequest;
import com.throughline.taskmanagement.enums.AssigneeType;
import com.throughline.taskmanagement.enums.Role;
import com.throughline.taskmanagement.enums.TaskStatus;
import com.throughline.taskmanagement.exception.ForbiddenActionException;
import com.throughline.taskmanagement.mapper.TaskMapper;
import com.throughline.taskmanagement.model.Department;
import com.throughline.taskmanagement.model.Person;
import com.throughline.taskmanagement.model.Task;
import com.throughline.taskmanagement.model.Team;
import com.throughline.taskmanagement.model.TeamMember;
import com.throughline.taskmanagement.repository.DepartmentRepository;
import com.throughline.taskmanagement.repository.PersonRepository;
import com.throughline.taskmanagement.repository.TaskCommentRepository;
import com.throughline.taskmanagement.repository.TaskDeadlineExtensionRequestRepository;
import com.throughline.taskmanagement.repository.TaskReassignmentRepository;
import com.throughline.taskmanagement.repository.TaskRepository;
import com.throughline.taskmanagement.repository.TaskActivityRepository;
import com.throughline.taskmanagement.repository.TeamMemberRepository;
import com.throughline.taskmanagement.repository.TeamRepository;
import com.throughline.taskmanagement.service.NotificationService;
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
 * Covers the authorization rules on TaskServiceImpl that used to have NO server-side check
 * at all (reassignTask) or were only gated by the frontend hiding a button (deleteTask) —
 * see the security-hardening pass this project went through — plus the later department-
 * headship restriction added on top of both: a plain Director only has authority within the
 * department they actually head (Department.headDirector), never merely one they belong to,
 * and never an unrelated one just by outranking a plain Member. Private checks
 * (requireCanReassign, requireCanDelete), so they're exercised here through the public
 * methods that call them, exactly as a real request would.
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceImplAuthorizationTest {

    @Mock private TaskRepository taskRepository;
    @Mock private PersonRepository personRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private TaskCommentRepository taskCommentRepository;
    @Mock private TaskReassignmentRepository taskReassignmentRepository;
    @Mock private TaskDeadlineExtensionRequestRepository taskDeadlineExtensionRequestRepository;
    @Mock private TaskMapper taskMapper;
    @Mock private NotificationService notificationService;
    @Mock private TaskActivityRepository taskActivityRepository;

    private TaskServiceImpl taskService;

    private Department ownDepartment;
    private Department otherDepartment;
    private Team owningTeam;
    private Team otherTeam;
    private Task topLevelTask;

    @BeforeEach
    void setUp() {
        taskService = new TaskServiceImpl(taskRepository, personRepository, teamRepository,
                teamMemberRepository, departmentRepository, taskCommentRepository, taskReassignmentRepository,
                taskDeadlineExtensionRequestRepository, taskMapper, notificationService, taskActivityRepository);

        ownDepartment = new Department();
        ownDepartment.setId(100L);
        ownDepartment.setName("Digital Banking Dept");

        otherDepartment = new Department();
        otherDepartment.setId(200L);
        otherDepartment.setName("Some Other Dept");

        owningTeam = new Team();
        owningTeam.setId(5L);
        owningTeam.setName("Digital Banking");
        owningTeam.setDepartment(ownDepartment);

        otherTeam = new Team();
        otherTeam.setId(6L);
        otherTeam.setName("Mobile Banking");

        topLevelTask = new Task();
        topLevelTask.setId(13L);
        topLevelTask.setParentTask(null);
        topLevelTask.setAssigneeType(AssigneeType.TEAM);
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
    void reassignTask_directorWhoHeadsThisTeamsDepartmentMayReassignIt() {
        Person director = personWithRole(1L, Role.DIRECTOR);
        ownDepartment.setHeadDirector(director);
        when(taskRepository.findWithDetailsById(13L)).thenReturn(Optional.of(topLevelTask));
        when(personRepository.findById(1L)).thenReturn(Optional.of(director));
        when(teamRepository.findById(6L)).thenReturn(Optional.of(otherTeam));

        ReassignTaskRequest request = new ReassignTaskRequest(6L, null, null, 1L, "handing off to Mobile Banking");

        assertDoesNotThrow(() -> taskService.reassignTask(13L, request));
    }

    @Test
    void reassignTask_directorWhoDoesNotHeadThisTeamsDepartmentIsForbiddenUnlessTeamLeader() {
        // Some other Director exists and is DIGITAL_BANKING_DEPT's actual head — this actor
        // outranks a plain Member but has no standing over a department they don't head.
        Person unrelatedDirector = personWithRole(2L, Role.DIRECTOR);
        ownDepartment.setHeadDirector(personWithRole(1L, Role.DIRECTOR));
        when(taskRepository.findWithDetailsById(13L)).thenReturn(Optional.of(topLevelTask));
        when(personRepository.findById(2L)).thenReturn(Optional.of(unrelatedDirector));
        when(teamMemberRepository.findByTeamIdAndIsLeaderTrue(5L)).thenReturn(Optional.empty());

        ReassignTaskRequest request = new ReassignTaskRequest(6L, null, null, 2L, "trying to hand this off");

        assertThrows(ForbiddenActionException.class, () -> taskService.reassignTask(13L, request));
        // Never even got to looking up the destination team — rejected purely on authority.
        verify(teamRepository, never()).findById(anyLong());
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

        ReassignTaskRequest request = new ReassignTaskRequest(6L, null, null, 10L, "handing off to Mobile Banking");

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

        ReassignTaskRequest request = new ReassignTaskRequest(6L, null, null, 11L, "trying to hand this off");

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

        ReassignTaskRequest request = new ReassignTaskRequest(6L, null, null, 12L, "trying to hand this off");

        assertThrows(ForbiddenActionException.class, () -> taskService.reassignTask(13L, request));
    }

    // ---- deleteTask ----

    @Test
    void deleteTask_directorMayDeleteATaskTheyCreatedThemselves() {
        Person director = personWithRole(1L, Role.DIRECTOR);
        ownDepartment.setHeadDirector(director);
        topLevelTask.setAssignedBy(director);
        when(personRepository.findById(1L)).thenReturn(Optional.of(director));
        when(taskRepository.findWithDetailsById(13L)).thenReturn(Optional.of(topLevelTask));

        assertDoesNotThrow(() -> taskService.deleteTask(13L, 1L));

        verify(taskRepository).delete(topLevelTask);
    }

    @Test
    void deleteTask_directorNoLongerHeadingTheDepartmentIsForbiddenEvenIfTheyCreatedIt() {
        // They created it back when they headed Digital Banking Dept, but headship has
        // since moved to someone else — the department, not creation alone, is what
        // requireCanDelete's second check is actually about.
        Person director = personWithRole(1L, Role.DIRECTOR);
        ownDepartment.setHeadDirector(personWithRole(2L, Role.DIRECTOR));
        topLevelTask.setAssignedBy(director);
        when(personRepository.findById(1L)).thenReturn(Optional.of(director));
        when(taskRepository.findWithDetailsById(13L)).thenReturn(Optional.of(topLevelTask));

        assertThrows(ForbiddenActionException.class, () -> taskService.deleteTask(13L, 1L));

        verify(taskRepository, never()).delete(any());
    }

    @Test
    void deleteTask_directorMayNotDeleteATaskSomeoneElseCreated() {
        // A Department task an Executive assigned, say — the receiving Director didn't
        // create it, just had it handed to them, so they can't delete it even though
        // they're Director-or-above.
        Person director = personWithRole(1L, Role.DIRECTOR);
        Person executive = personWithRole(20L, Role.EXECUTIVE);
        topLevelTask.setAssignedBy(executive);
        when(personRepository.findById(1L)).thenReturn(Optional.of(director));
        when(taskRepository.findWithDetailsById(13L)).thenReturn(Optional.of(topLevelTask));

        assertThrows(ForbiddenActionException.class, () -> taskService.deleteTask(13L, 1L));

        verify(taskRepository, never()).delete(any());
    }

    @Test
    void deleteTask_superAdminMayDeleteRegardlessOfWhoCreatedIt() {
        Person superAdmin = personWithRole(19L, Role.SUPER_ADMIN);
        Person someoneElse = personWithRole(1L, Role.DIRECTOR);
        topLevelTask.setAssignedBy(someoneElse);
        when(personRepository.findById(19L)).thenReturn(Optional.of(superAdmin));
        when(taskRepository.findWithDetailsById(13L)).thenReturn(Optional.of(topLevelTask));

        assertDoesNotThrow(() -> taskService.deleteTask(13L, 19L));

        verify(taskRepository).delete(topLevelTask);
    }

    @Test
    void deleteTask_executiveMayDeleteRegardlessOfWhoCreatedIt() {
        Person executive = personWithRole(20L, Role.EXECUTIVE);
        Person someoneElse = personWithRole(1L, Role.DIRECTOR);
        topLevelTask.setAssignedBy(someoneElse);
        when(personRepository.findById(20L)).thenReturn(Optional.of(executive));
        when(taskRepository.findWithDetailsById(13L)).thenReturn(Optional.of(topLevelTask));

        assertDoesNotThrow(() -> taskService.deleteTask(13L, 20L));

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
