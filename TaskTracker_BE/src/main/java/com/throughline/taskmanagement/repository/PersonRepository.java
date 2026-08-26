package com.throughline.taskmanagement.repository;

import com.throughline.taskmanagement.model.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PersonRepository extends JpaRepository<Person, Long> {
    Optional<Person> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    List<Person> findByTeamId(Long teamId);

    @Query("SELECT p FROM Person p WHERE LOWER(p.fullName) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(p.role) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Person> search(@Param("q") String q);
}
