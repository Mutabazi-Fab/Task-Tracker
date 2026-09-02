package com.throughline.taskmanagement.controller;

import com.throughline.taskmanagement.dto.request.AddCommentRequest;
import com.throughline.taskmanagement.dto.request.CreateTaskRequest;
import com.throughline.taskmanagement.dto.request.ReassignTaskRequest;
import com.throughline.taskmanagement.enums.Role;
import com.throughline.taskmanagement.enums.TaskStatus;
import com.throughline.taskmanagement.model.Person;
import com.throughline.taskmanagement.security.CurrentPersonResolver;
import com.throughline.taskmanagement.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The core proof of this project's security-hardening pass: a client can put anyone's id
 * in a request body's "who's doing this" field, but the controller must always overwrite
 * it with whoever the JWT actually says is logged in before the service ever sees it. Each
 * test below deliberately sends a spoofed id and asserts the service received the REAL one
 * instead — exactly the scenario proven live (via curl) during that work.
 */
@ExtendWith(MockitoExtension.class)
class TaskControllerActorSubstitutionTest {

    @Mock private TaskService taskService;
    @Mock private CurrentPersonResolver currentPersonResolver;
    @Mock private Authentication authentication;

    private TaskController controller;

    private static final long REAL_ACTOR_ID = 4L; // Vincent, a plain Member
    private static final long SPOOFED_ACTOR_ID = 19L; // the Super Admin's real id

    @BeforeEach
    void setUp() {
        controller = new TaskController(taskService, currentPersonResolver);
        // Not every test below goes through resolveId (getAllTasks's scoping helper calls
        // resolve() instead) — lenient so those tests don't fail Mockito's unused-stub check.
        lenient().when(currentPersonResolver.resolveId(authentication)).thenReturn(REAL_ACTOR_ID);
    }

    @Test
    void createTask_ignoresASpoofedCreatedById() {
        CreateTaskRequest spoofed = new CreateTaskRequest(
                "Spoofed task", "desc", SPOOFED_ACTOR_ID, 5L, LocalDate.now(), "opening note");

        controller.createTask(spoofed, authentication);

        ArgumentCaptor<CreateTaskRequest> captor = ArgumentCaptor.forClass(CreateTaskRequest.class);
        verify(taskService).createTask(captor.capture());
        assertEquals(REAL_ACTOR_ID, captor.getValue().createdById());
    }

    @Test
    void reassignTask_ignoresASpoofedReassignedById() {
        ReassignTaskRequest spoofed = new ReassignTaskRequest(6L, null, SPOOFED_ACTOR_ID, "handing off");

        controller.reassignTask(13L, spoofed, authentication);

        ArgumentCaptor<ReassignTaskRequest> captor = ArgumentCaptor.forClass(ReassignTaskRequest.class);
        verify(taskService).reassignTask(eq(13L), captor.capture());
        assertEquals(REAL_ACTOR_ID, captor.getValue().reassignedById());
    }

    @Test
    void addProgressComment_ignoresASpoofedAuthorId() {
        AddCommentRequest spoofed = new AddCommentRequest(SPOOFED_ACTOR_ID, 50, "made great progress");

        controller.addProgressComment(9L, spoofed, authentication);

        ArgumentCaptor<AddCommentRequest> captor = ArgumentCaptor.forClass(AddCommentRequest.class);
        verify(taskService).addProgressComment(eq(9L), captor.capture());
        assertEquals(REAL_ACTOR_ID, captor.getValue().authorId());
    }

    @Test
    void deleteTask_alwaysUsesTheRealResolvedActor() {
        controller.deleteTask(13L, authentication);

        verify(taskService).deleteTask(13L, REAL_ACTOR_ID);
    }

    @Test
    void getAllTasks_forcesAMemberToTheirOwnTasksEvenIfTheyAskForSomeoneElses() {
        Person member = new Person();
        member.setId(REAL_ACTOR_ID);
        member.setRole(Role.MEMBER);
        when(currentPersonResolver.resolve(authentication)).thenReturn(member);
        when(taskService.getAllTasks(null, REAL_ACTOR_ID, Pageable.unpaged())).thenReturn(Page.empty());

        // Asking for someone else's tasks (id 99) by leaving the door open in the query param.
        controller.getAllTasks(null, 99L, Pageable.unpaged(), authentication);

        verify(taskService).getAllTasks(null, REAL_ACTOR_ID, Pageable.unpaged());
    }

    @Test
    void getAllTasks_letsADirectorSeeWhoeverTheyAskFor() {
        Person director = new Person();
        director.setId(1L);
        director.setRole(Role.DIRECTOR);
        when(currentPersonResolver.resolve(authentication)).thenReturn(director);
        when(taskService.getAllTasks(null, 99L, Pageable.unpaged())).thenReturn(Page.empty());

        controller.getAllTasks(null, 99L, Pageable.unpaged(), authentication);

        verify(taskService).getAllTasks(null, 99L, Pageable.unpaged());
    }

    @Test
    void getAllTasks_directorSeesEverythingWhenNoFilterIsGiven() {
        Person director = new Person();
        director.setId(1L);
        director.setRole(Role.DIRECTOR);
        when(currentPersonResolver.resolve(authentication)).thenReturn(director);
        when(taskService.getAllTasks(TaskStatus.ONGOING, null, Pageable.unpaged())).thenReturn(Page.empty());

        controller.getAllTasks(TaskStatus.ONGOING, null, Pageable.unpaged(), authentication);

        verify(taskService).getAllTasks(TaskStatus.ONGOING, null, Pageable.unpaged());
    }
}
