package com.throughline.taskmanagement.service.impl;

import com.throughline.taskmanagement.dto.request.AddTeamMemberRequest;
import com.throughline.taskmanagement.dto.request.RemoveTeamMemberRequest;
import com.throughline.taskmanagement.dto.request.SetTeamLeaderRequest;
import com.throughline.taskmanagement.exception.ForbiddenActionException;
import com.throughline.taskmanagement.model.Team;
import com.throughline.taskmanagement.repository.PersonRepository;
import com.throughline.taskmanagement.repository.TeamMemberRepository;
import com.throughline.taskmanagement.repository.TeamRepository;
import com.throughline.taskmanagement.service.TeamService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Runs against the real seeded database, wrapped in @Transactional so the membership change
 * it actually performs (the positive case) rolls back at the end.
 *
 * Proves the exact bug this was written to fix: Jean Paul Ndayambaje heads Information
 * Technology, not Finance — he could previously add/remove members and reassign the leader
 * on Finance's "Financial Planning & Analysis" team despite that, since TeamServiceImpl only
 * ever checked "is this any Director", never which department they actually head. He should
 * still be able to do all of that on his own department's teams.
 */
@SpringBootTest
@Transactional
class TeamServiceImplDepartmentAuthorizationTest {

    @Autowired
    private TeamService teamService;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    private Long idOf(String email) {
        return personRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Seed data missing: " + email))
                .getId();
    }

    private Long teamIdByName(String name) {
        return teamRepository.findAll().stream()
                .filter(t -> t.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Seed data missing team: " + name))
                .getId();
    }

    @Test
    void directorCannotAddAMemberToAnotherDepartmentsTeam() {
        Long jeanPaulId = idOf("jeanpaul.ndayambaje@example.com"); // heads IT, not Finance
        Long financeTeamId = teamIdByName("Financial Planning & Analysis"); // headed by Théogène Habimana
        Long beatriceId = idOf("beatrice.uwamahoro@example.com"); // Finance dept, not yet on this team

        assertThrows(ForbiddenActionException.class, () -> teamService.addMember(
                financeTeamId, new AddTeamMemberRequest(beatriceId, jeanPaulId, "trying to add across departments")));
    }

    @Test
    void directorCannotRemoveAMemberFromAnotherDepartmentsTeam() {
        Long jeanPaulId = idOf("jeanpaul.ndayambaje@example.com");
        Team financeTeam = teamRepository.findById(teamIdByName("Financial Planning & Analysis")).orElseThrow();
        Long anyFinanceMemberId = teamMemberRepository.findByTeamId(financeTeam.getId()).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Seed data missing: Finance team has no members"))
                .getPerson().getId();

        assertThrows(ForbiddenActionException.class, () -> teamService.removeMember(
                financeTeam.getId(), anyFinanceMemberId,
                new RemoveTeamMemberRequest(jeanPaulId, "trying to remove across departments")));
    }

    @Test
    void directorCannotReassignAnotherDepartmentsTeamLeader() {
        Long jeanPaulId = idOf("jeanpaul.ndayambaje@example.com");
        Team financeTeam = teamRepository.findById(teamIdByName("Financial Planning & Analysis")).orElseThrow();
        Long anyFinanceMemberId = teamMemberRepository.findByTeamId(financeTeam.getId()).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Seed data missing: Finance team has no members"))
                .getPerson().getId();

        assertThrows(ForbiddenActionException.class, () -> teamService.setTeamLeader(
                financeTeam.getId(), anyFinanceMemberId, new SetTeamLeaderRequest(jeanPaulId)));
    }

    @Test
    void directorMayStillAddAMemberToTheirOwnDepartmentsTeam() {
        Long jeanPaulId = idOf("jeanpaul.ndayambaje@example.com");
        Long digitalBankingTeamId = teamIdByName("Digital Banking"); // IT — Jean Paul's own department
        Long solangeId = idOf("solange.umutoni@example.com"); // IT dept, not yet on this team

        assertDoesNotThrow(() -> teamService.addMember(
                digitalBankingTeamId, new AddTeamMemberRequest(solangeId, jeanPaulId, "adding within own department")));
    }
}
