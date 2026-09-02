package com.throughline.taskmanagement.controller;

import com.throughline.taskmanagement.dto.request.ChangeRoleRequest;
import com.throughline.taskmanagement.dto.request.CreatePersonRequest;
import com.throughline.taskmanagement.dto.request.SendPasswordResetRequest;
import com.throughline.taskmanagement.dto.request.SetAccountActiveRequest;
import com.throughline.taskmanagement.enums.Role;
import com.throughline.taskmanagement.security.CurrentPersonResolver;
import com.throughline.taskmanagement.service.PersonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Same principle as TaskControllerActorSubstitutionTest, applied to the endpoints that
 *  change someone's role, lock/unlock their account, or trigger a password reset on their
 *  behalf — arguably the highest-stakes places a spoofed actor id could matter. */
@ExtendWith(MockitoExtension.class)
class PersonControllerActorSubstitutionTest {

    @Mock private PersonService personService;
    @Mock private CurrentPersonResolver currentPersonResolver;
    @Mock private Authentication authentication;

    private PersonController controller;

    private static final long REAL_ACTOR_ID = 4L; // a plain Member, really logged in
    private static final long SPOOFED_ACTOR_ID = 19L; // the Super Admin's real id, claimed falsely

    @BeforeEach
    void setUp() {
        controller = new PersonController(personService, currentPersonResolver);
        when(currentPersonResolver.resolveId(authentication)).thenReturn(REAL_ACTOR_ID);
    }

    @Test
    void changeRole_ignoresASpoofedChangedById() {
        ChangeRoleRequest spoofed = new ChangeRoleRequest(Role.DIRECTOR, SPOOFED_ACTOR_ID, "promoting myself");

        controller.changeRole(7L, spoofed, authentication);

        ArgumentCaptor<ChangeRoleRequest> captor = ArgumentCaptor.forClass(ChangeRoleRequest.class);
        verify(personService).changeRole(eq(7L), captor.capture());
        assertEquals(REAL_ACTOR_ID, captor.getValue().changedById());
    }

    @Test
    void setActive_ignoresASpoofedChangedById() {
        SetAccountActiveRequest spoofed = new SetAccountActiveRequest(false, SPOOFED_ACTOR_ID, "deactivating a rival");

        controller.setActive(7L, spoofed, authentication);

        ArgumentCaptor<SetAccountActiveRequest> captor = ArgumentCaptor.forClass(SetAccountActiveRequest.class);
        verify(personService).setActive(eq(7L), captor.capture());
        assertEquals(REAL_ACTOR_ID, captor.getValue().changedById());
    }

    @Test
    void sendPasswordReset_ignoresASpoofedChangedById() {
        SendPasswordResetRequest spoofed = new SendPasswordResetRequest(SPOOFED_ACTOR_ID, "helping them recover");

        controller.sendPasswordReset(7L, spoofed, authentication);

        ArgumentCaptor<SendPasswordResetRequest> captor = ArgumentCaptor.forClass(SendPasswordResetRequest.class);
        verify(personService).sendPasswordReset(eq(7L), captor.capture());
        assertEquals(REAL_ACTOR_ID, captor.getValue().changedById());
    }

    @Test
    void createPerson_ignoresASpoofedCreatedById() {
        CreatePersonRequest spoofed = new CreatePersonRequest(
                "New Person", "new@example.com", "Engineer", null, SPOOFED_ACTOR_ID, Role.SUPER_ADMIN);

        controller.createPerson(spoofed, authentication);

        ArgumentCaptor<CreatePersonRequest> captor = ArgumentCaptor.forClass(CreatePersonRequest.class);
        verify(personService).createPerson(captor.capture());
        assertEquals(REAL_ACTOR_ID, captor.getValue().createdById());
    }

    @Test
    void getPersonById_passesTheRealResolvedViewerNotAnythingFromTheRequest() {
        controller.getPersonById(7L, authentication);

        verify(personService).getPersonById(7L, REAL_ACTOR_ID);
    }
}
