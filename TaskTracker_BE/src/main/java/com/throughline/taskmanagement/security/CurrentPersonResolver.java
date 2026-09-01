package com.throughline.taskmanagement.security;

import com.throughline.taskmanagement.exception.ResourceNotFoundException;
import com.throughline.taskmanagement.model.Person;
import com.throughline.taskmanagement.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * The one place that turns a verified login token into a real Person — every controller
 * that used to trust a client-supplied "who's doing this" id (createdById, changedById,
 * assignedPersonId, personId, ...) resolves it from here instead, using the email JWT
 * already proved belongs to this request (Authentication.getName()). A request can no
 * longer claim to be someone else just by putting a different id in the body or a query
 * param — the id it's allowed to act as is whichever real, logged-in person it actually is.
 */
@Component
@RequiredArgsConstructor
public class CurrentPersonResolver {

    private final PersonRepository personRepository;

    public Person resolve(Authentication authentication) {
        return personRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Person not found"));
    }

    public Long resolveId(Authentication authentication) {
        return resolve(authentication).getId();
    }
}
