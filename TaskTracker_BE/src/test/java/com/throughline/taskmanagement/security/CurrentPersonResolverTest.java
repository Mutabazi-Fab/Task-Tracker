package com.throughline.taskmanagement.security;

import com.throughline.taskmanagement.exception.ResourceNotFoundException;
import com.throughline.taskmanagement.model.Person;
import com.throughline.taskmanagement.repository.PersonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * This is the one place every "who's really logged in" check in the app relies on — every
 * actor-spoofing protection built on top of it depends on this resolving the REAL person
 * from the JWT-verified email (Authentication.getName()), never from anything a client
 * could otherwise influence.
 */
@ExtendWith(MockitoExtension.class)
class CurrentPersonResolverTest {

    @Mock
    private PersonRepository personRepository;

    @Mock
    private Authentication authentication;

    private CurrentPersonResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new CurrentPersonResolver(personRepository);
    }

    @Test
    void resolvesThePersonMatchingTheAuthenticatedEmail() {
        Person person = new Person();
        person.setId(4L);
        person.setEmail("vincent.byiringiro@example.com");

        when(authentication.getName()).thenReturn("vincent.byiringiro@example.com");
        when(personRepository.findByEmailIgnoreCase("vincent.byiringiro@example.com"))
                .thenReturn(Optional.of(person));

        Person resolved = resolver.resolve(authentication);

        assertEquals(4L, resolved.getId());
    }

    @Test
    void resolveIdReturnsJustTheId() {
        Person person = new Person();
        person.setId(19L);

        when(authentication.getName()).thenReturn("mucyomutabazifabrice@gmail.com");
        when(personRepository.findByEmailIgnoreCase("mucyomutabazifabrice@gmail.com"))
                .thenReturn(Optional.of(person));

        assertEquals(19L, resolver.resolveId(authentication));
    }

    @Test
    void throwsWhenNoPersonMatchesTheAuthenticatedEmail() {
        when(authentication.getName()).thenReturn("nobody@example.com");
        when(personRepository.findByEmailIgnoreCase("nobody@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> resolver.resolve(authentication));
    }
}
