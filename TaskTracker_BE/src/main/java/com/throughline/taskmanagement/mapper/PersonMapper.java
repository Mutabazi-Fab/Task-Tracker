package com.throughline.taskmanagement.mapper;

import com.throughline.taskmanagement.dto.response.PersonResponse;
import com.throughline.taskmanagement.model.Person;
import org.springframework.stereotype.Component;

@Component
public class PersonMapper {

    public PersonResponse toResponse(Person person) {
        if (person == null) {
            return null;
        }
        return new PersonResponse(
                person.getId(),
                person.getFullName(),
                person.getEmail(),
                person.getRole(),
                person.getRank(),
                person.getTeam() != null ? person.getTeam().getName() : null,
                person.getTeam() != null ? person.getTeam().getId() : null
        );
    }
}
