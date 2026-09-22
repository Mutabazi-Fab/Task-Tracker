package com.throughline.taskmanagement.repository;

import com.throughline.taskmanagement.model.PersonDailyGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PersonDailyGoalRepository extends JpaRepository<PersonDailyGoal, Long> {

    List<PersonDailyGoal> findByPersonIdOrderByAddedAtAsc(Long personId);

    long countByPersonId(Long personId);

    Optional<PersonDailyGoal> findByPersonIdAndTaskId(Long personId, Long taskId);
}
