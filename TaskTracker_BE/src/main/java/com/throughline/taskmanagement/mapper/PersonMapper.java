package com.throughline.taskmanagement.mapper;

import com.throughline.taskmanagement.dto.response.PersonResponse;
import com.throughline.taskmanagement.dto.response.PersonTeamMembershipResponse;
import com.throughline.taskmanagement.model.Person;
import com.throughline.taskmanagement.model.TeamMember;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PersonMapper {

    /**
     * memberships is passed in rather than looked up here — a mapper stays a pure
     * entity-to-DTO transform with no repository access; the caller (PersonServiceImpl)
     * fetches this person's TeamMember rows and hands them over.
     */
    public PersonResponse toResponse(Person person, List<TeamMember> memberships) {
        if (person == null) {
            return null;
        }

        List<PersonTeamMembershipResponse> teams = memberships == null
                ? List.of()
                : memberships.stream()
                        .map(m -> new PersonTeamMembershipResponse(m.getTeam().getId(), m.getTeam().getName(), m.isLeader()))
                        .toList();

        return new PersonResponse(
                person.getId(),
                person.getFullName(),
                person.getEmail(),
                person.getJobTitle(),
                person.getRank(),
                person.getRole(),
                person.isEmailVerified(),
                person.isActive(),
                teams
        );
    }
}
