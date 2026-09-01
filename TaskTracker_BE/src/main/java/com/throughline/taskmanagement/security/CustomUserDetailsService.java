package com.throughline.taskmanagement.security;

import com.throughline.taskmanagement.model.Person;
import com.throughline.taskmanagement.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final PersonRepository personRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Person person = personRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("No account for " + email));

        if (person.getPassword() == null) {
            // Exists in the system (e.g. seeded before auth existed) but never signed up —
            // treated as "no such login" rather than exposing that the email is known.
            throw new UsernameNotFoundException("Account has not been provisioned for login yet");
        }

        // Re-checked on EVERY request (this filter re-loads UserDetails per request, not
        // once at login) — so an account deactivated mid-session stops working immediately,
        // not just for its next fresh login. JwtAuthenticationFilter's catch-all treats any
        // exception here as "leave the request unauthenticated", which is exactly right.
        if (!person.isActive()) {
            throw new DisabledException("Account is deactivated");
        }

        String roleName = person.getRole() != null ? person.getRole().name() : "MEMBER";

        return new User(
                person.getEmail(),
                person.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + roleName))
        );
    }
}
