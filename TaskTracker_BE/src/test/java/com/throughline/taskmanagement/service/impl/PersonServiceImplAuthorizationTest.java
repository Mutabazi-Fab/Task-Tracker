package com.throughline.taskmanagement.service.impl;

import com.throughline.taskmanagement.dto.request.ChangeRoleRequest;
import com.throughline.taskmanagement.dto.request.CreatePersonRequest;
import com.throughline.taskmanagement.dto.request.SendPasswordResetRequest;
import com.throughline.taskmanagement.dto.request.SetAccountActiveRequest;
import com.throughline.taskmanagement.enums.Role;
import com.throughline.taskmanagement.exception.ForbiddenActionException;
import com.throughline.taskmanagement.exception.InvalidAssignmentException;
import com.throughline.taskmanagement.mapper.PersonMapper;
import com.throughline.taskmanagement.mapper.TaskMapper;
import com.throughline.taskmanagement.model.Department;
import com.throughline.taskmanagement.model.Person;
import com.throughline.taskmanagement.repository.AccountStatusChangeRepository;
import com.throughline.taskmanagement.repository.DepartmentRepository;
import com.throughline.taskmanagement.repository.PersonDailyGoalRepository;
import com.throughline.taskmanagement.repository.PersonRepository;
import com.throughline.taskmanagement.repository.RoleChangeRepository;
import com.throughline.taskmanagement.repository.TaskCommentRepository;
import com.throughline.taskmanagement.repository.TaskRepository;
import com.throughline.taskmanagement.repository.TeamMemberRepository;
import com.throughline.taskmanagement.service.AuthService;
import com.throughline.taskmanagement.service.MailService;
import com.throughline.taskmanagement.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Covers requireCanViewPerson (self/teammate/Director allowed, an unrelated Member
 * forbidden — this used to be wide open to anyone authenticated, by ID) and the
 * Super-Admin-only gates on changeRole/setActive/sendPasswordReset.
 */
@ExtendWith(MockitoExtension.class)
class PersonServiceImplAuthorizationTest {

    @Mock private PersonRepository personRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private TaskCommentRepository taskCommentRepository;
    @Mock private RoleChangeRepository roleChangeRepository;
    @Mock private AccountStatusChangeRepository accountStatusChangeRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private PersonDailyGoalRepository personDailyGoalRepository;
    @Mock private PersonMapper personMapper;
    @Mock private TaskMapper taskMapper;
    @Mock private NotificationService notificationService;
    @Mock private MailService mailService;
    @Mock private AuthService authService;
    @Mock private PasswordEncoder passwordEncoder;

    private PersonServiceImpl personService;

    @BeforeEach
    void setUp() {
        personService = new PersonServiceImpl(personRepository, teamMemberRepository, taskRepository,
                taskCommentRepository, roleChangeRepository, accountStatusChangeRepository, departmentRepository,
                personDailyGoalRepository, personMapper, taskMapper, notificationService, mailService, authService,
                passwordEncoder);
        // Only some tests exercise getPersonStatistics/getPersonTaskHistory's downstream
        // repository calls, but requireCanViewPerson runs first in all three — stub the
        // harmless empty-list ones leniently so tests that never reach them don't fail
        // Mockito's unnecessary-stub check.
        lenient().when(taskRepository.findByAssignedPersonId(anyLong())).thenReturn(Collections.emptyList());
        lenient().when(taskRepository.getAverageProgressByAssignedPersonId(anyLong())).thenReturn(0.0);
        lenient().when(taskRepository.findTasksConnectedToPerson(anyLong())).thenReturn(Collections.emptyList());
        lenient().when(teamMemberRepository.findByPersonId(anyLong())).thenReturn(Collections.emptyList());
    }

    private Person personWithRole(long id, Role role) {
        Person person = new Person();
        person.setId(id);
        person.setRole(role);
        return person;
    }

    // ---- getPersonById / requireCanViewPerson ----

    @Test
    void viewingYourOwnProfileNeedsNoTeamOverlap() {
        Person self = personWithRole(4L, Role.MEMBER);
        when(personRepository.findById(4L)).thenReturn(Optional.of(self));

        assertDoesNotThrow(() -> personService.getPersonById(4L, 4L));
    }

    @Test
    void directorMayViewAnyonesProfile() {
        Person director = personWithRole(1L, Role.DIRECTOR);
        Person target = personWithRole(7L, Role.MEMBER);
        when(personRepository.findById(1L)).thenReturn(Optional.of(director));
        when(personRepository.findById(7L)).thenReturn(Optional.of(target));

        assertDoesNotThrow(() -> personService.getPersonById(7L, 1L));
    }

    @Test
    void teammateMayViewEachOthersProfile() {
        Person viewer = personWithRole(4L, Role.MEMBER);
        Person target = personWithRole(3L, Role.MEMBER);
        when(personRepository.findById(4L)).thenReturn(Optional.of(viewer));
        when(personRepository.findById(3L)).thenReturn(Optional.of(target));
        when(teamMemberRepository.existsSharedTeam(4L, 3L)).thenReturn(true);

        assertDoesNotThrow(() -> personService.getPersonById(3L, 4L));
    }

    @Test
    void unrelatedMemberIsForbiddenFromViewingSomeoneElsesProfile() {
        Person viewer = personWithRole(4L, Role.MEMBER);
        when(personRepository.findById(4L)).thenReturn(Optional.of(viewer));
        when(teamMemberRepository.existsSharedTeam(4L, 7L)).thenReturn(false);

        assertThrows(ForbiddenActionException.class, () -> personService.getPersonById(7L, 4L));
    }

    // ---- changeRole ----

    @Test
    void superAdminMayChangeRoles() {
        Person superAdmin = personWithRole(19L, Role.SUPER_ADMIN);
        Person target = personWithRole(4L, Role.MEMBER);
        when(personRepository.findById(19L)).thenReturn(Optional.of(superAdmin));
        when(personRepository.findById(4L)).thenReturn(Optional.of(target));

        ChangeRoleRequest request = new ChangeRoleRequest(Role.DIRECTOR, 19L, "promoting for the new initiative");

        assertDoesNotThrow(() -> personService.changeRole(4L, request));
    }

    @Test
    void directorMayNotChangeRoles() {
        Person director = personWithRole(1L, Role.DIRECTOR);
        when(personRepository.findById(1L)).thenReturn(Optional.of(director));

        ChangeRoleRequest request = new ChangeRoleRequest(Role.DIRECTOR, 1L, "trying to promote someone");

        assertThrows(ForbiddenActionException.class, () -> personService.changeRole(4L, request));
    }

    // ---- setActive ----

    @Test
    void directorMayNotDeactivateAccounts() {
        Person director = personWithRole(1L, Role.DIRECTOR);
        when(personRepository.findById(1L)).thenReturn(Optional.of(director));

        SetAccountActiveRequest request = new SetAccountActiveRequest(false, 1L, "trying to deactivate someone");

        assertThrows(ForbiddenActionException.class, () -> personService.setActive(4L, request));
    }

    // ---- sendPasswordReset ----

    @Test
    void memberMayNotTriggerSomeoneElsesPasswordReset() {
        Person member = personWithRole(4L, Role.MEMBER);
        when(personRepository.findById(4L)).thenReturn(Optional.of(member));

        SendPasswordResetRequest request = new SendPasswordResetRequest(4L, "trying to reset Claudine's password");

        assertThrows(ForbiddenActionException.class, () -> personService.sendPasswordReset(7L, request));
    }

    @Test
    void superAdminCannotResetAPasswordForSomeoneWhoNeverSignedUp() {
        Person superAdmin = personWithRole(19L, Role.SUPER_ADMIN);
        Person neverSignedUp = personWithRole(5L, Role.MEMBER);
        neverSignedUp.setPassword(null);
        when(personRepository.findById(19L)).thenReturn(Optional.of(superAdmin));
        when(personRepository.findById(5L)).thenReturn(Optional.of(neverSignedUp));

        SendPasswordResetRequest request = new SendPasswordResetRequest(19L, "helping them get in");

        assertThrows(InvalidAssignmentException.class, () -> personService.sendPasswordReset(5L, request));
    }

    // ---- createPerson ----
    // There is no public self-registration — only a Super Admin may create a new,
    // login-enabled account at all (not just a non-Member one), and the password they set
    // must come through hashed and the account must start usable (emailVerified=true),
    // since there's no more OTP step to unlock it afterward.

    @Test
    void superAdminMayCreateANewPerson() {
        Person superAdmin = personWithRole(19L, Role.SUPER_ADMIN);
        Department department = new Department();
        department.setId(2L);
        when(personRepository.findById(19L)).thenReturn(Optional.of(superAdmin));
        when(personRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(department));
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password123");
        when(personRepository.save(any(Person.class))).thenAnswer(inv -> inv.getArgument(0));

        CreatePersonRequest request = new CreatePersonRequest(
                "New Person", "new@example.com", "Engineer", null, 19L, Role.MEMBER, 2L, "password123");

        assertDoesNotThrow(() -> personService.createPerson(request));

        org.mockito.ArgumentCaptor<Person> captor = org.mockito.ArgumentCaptor.forClass(Person.class);
        org.mockito.Mockito.verify(personRepository).save(captor.capture());
        assertEquals("hashed-password123", captor.getValue().getPassword());
        assertTrue(captor.getValue().isEmailVerified());
    }

    @Test
    void directorMayNotCreateANewPerson() {
        Person director = personWithRole(1L, Role.DIRECTOR);
        when(personRepository.findById(1L)).thenReturn(Optional.of(director));

        CreatePersonRequest request = new CreatePersonRequest(
                "New Person", "new@example.com", "Engineer", null, 1L, null, 2L, "password123");

        assertThrows(ForbiddenActionException.class, () -> personService.createPerson(request));
    }

    @Test
    void createPersonRejectsAShortPassword() {
        Person superAdmin = personWithRole(19L, Role.SUPER_ADMIN);
        Department department = new Department();
        department.setId(2L);
        when(personRepository.findById(19L)).thenReturn(Optional.of(superAdmin));
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(department));

        CreatePersonRequest request = new CreatePersonRequest(
                "New Person", "new@example.com", "Engineer", null, 19L, null, 2L, "short");

        assertThrows(InvalidAssignmentException.class, () -> personService.createPerson(request));
    }

    // ---- getAllPeople scoping ----

    @Test
    void memberSeesOnlyTeammatesOnThePeopleList() {
        Person member = personWithRole(4L, Role.MEMBER);
        when(personRepository.findById(4L)).thenReturn(Optional.of(member));
        when(personRepository.findTeammatesOf(anyLong(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(org.springframework.data.domain.Page.empty());

        personService.getAllPeople(4L, org.springframework.data.domain.Pageable.unpaged());

        org.mockito.Mockito.verify(personRepository, org.mockito.Mockito.never())
                .findAll(org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    void directorSeesEveryoneOnThePeopleList() {
        Person director = personWithRole(1L, Role.DIRECTOR);
        when(personRepository.findById(1L)).thenReturn(Optional.of(director));
        when(personRepository.findAll(org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(org.springframework.data.domain.Page.empty());

        personService.getAllPeople(1L, org.springframework.data.domain.Pageable.unpaged());

        org.mockito.Mockito.verify(personRepository, org.mockito.Mockito.never())
                .findTeammatesOf(anyLong(), org.mockito.ArgumentMatchers.any());
    }
}
