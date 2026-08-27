package com.throughline.taskmanagement.service.impl;

import com.throughline.taskmanagement.dto.request.LoginRequest;
import com.throughline.taskmanagement.dto.request.SignupRequest;
import com.throughline.taskmanagement.dto.response.AuthResponse;
import com.throughline.taskmanagement.dto.response.PersonResponse;
import com.throughline.taskmanagement.enums.Role;
import com.throughline.taskmanagement.exception.DuplicateResourceException;
import com.throughline.taskmanagement.exception.InvalidCredentialsException;
import com.throughline.taskmanagement.exception.ResourceNotFoundException;
import com.throughline.taskmanagement.mapper.PersonMapper;
import com.throughline.taskmanagement.model.Person;
import com.throughline.taskmanagement.repository.PersonRepository;
import com.throughline.taskmanagement.repository.TeamMemberRepository;
import com.throughline.taskmanagement.security.JwtService;
import com.throughline.taskmanagement.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final PersonRepository personRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final PersonMapper personMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public AuthResponse signup(SignupRequest request) {
        Person person = personRepository.findByEmailIgnoreCase(request.email()).orElse(null);

        if (person != null && person.getPassword() != null) {
            throw new DuplicateResourceException("An account with this email already exists.");
        }

        if (person == null) {
            person = new Person();
            person.setEmail(request.email());
        }
        // else: this claims a pre-existing record (e.g. seeded before auth existed, or added
        // to a team by a Director before ever signing up) instead of creating a duplicate
        // person with the same email.

        person.setFullName(request.fullName());
        person.setJobTitle(request.jobTitle());
        person.setRank(request.rank());
        person.setPassword(passwordEncoder.encode(request.password()));
        if (person.getRole() == null) {
            person.setRole(Role.MEMBER);
        }

        Person saved = personRepository.save(person);
        String token = jwtService.generateToken(saved.getEmail());

        return new AuthResponse(token, saved.getId(), saved.getFullName(), saved.getEmail(), saved.getRole());
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Person person = personRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password."));

        if (person.getPassword() == null || !passwordEncoder.matches(request.password(), person.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password.");
        }

        String token = jwtService.generateToken(person.getEmail());
        return new AuthResponse(token, person.getId(), person.getFullName(), person.getEmail(), person.getRole());
    }

    @Override
    public PersonResponse getCurrentPerson(String email) {
        Person person = personRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Person not found"));
        return personMapper.toResponse(person, teamMemberRepository.findByPersonId(person.getId()));
    }
}
